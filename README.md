# Quick start

This repository in general covers entire L1 API, but if you want to start quickly - see `velox.api.layer1.simplified.demo` subpackage - those are "Simplified API" demos, it's the quickest way to get started.

Word of caution: please be very careful when using something with real account. Always test in simulation first. Both your code and API might contain bugs, so before you run anything you have modified/wrote/downloaded from somewhere in live - make sure to validate that it works fine in paper trading/simulated environment. Please be aware, that bookmap simulation has unrealistically low latency (everything is done on local machine), so you should do the testing with a server-side demo account before even considering running anything in live.

## Building your adapter

- Make sure you have gradle installed and added to your path variable. If not - see https://gradle.org/install/
- Make sure that your gradle is set to use Java 8
- Clone the repository: `git clone https://github.com/BookmapAPI/DemoStrategies.git`
- Go to `Strategies` subfolder of the repository (the one with `build.gradle` file in it).
- Run `gradle jar`. If everything was done correctly - gradle will tell you that build was successful.
- In `Strategies/build/libs` subfolder (relative to the root of the repository) you should now have `bm-strategies.jar` - those are your indicators and strategies compiled and ready to be loaded into Bookmap

If you have Gradle installed and configured to use Java 8, you can simply run these commands:
```
git clone https://github.com/BookmapAPI/DemoStrategies.git
cd DemoStrategies/Strategies
gradle jar
```
And you'll have your jar file that is ready to be loaded inside `build/libs`.

## Loading into bookmap

In Bookmap go to "Settings"->"Api plugins configuration" (or click a corresponding toolbar button) and press "Add". Select your newly compiled Jar file and pick an addon you'd like to load in popup window.

Try "Last trade: live" if you are interested in "Simplified API" or "Markers demo" if you want the core API.

Module will appear in the window, now you just have to enable it using the checkbox on the left.

## What should I do next?

Try making some modifications to the examples to see how things work. Change the code, invoke `gradle jar` again, restart bookmap - you should now be running with new version of your code (bookmap doesn't cache your classes anywhere).

You might also want to set up an IDE - this will be covered later in this guide.

### Making sure your changes get applied
**Important note: this specific demo project is compiled as part of Bookmap itself. It means that if you want to make changes to classes in this project you should either rename those or delete built-in `bm-strategies.jar`**. Failing to do that will result in Bookmap loading built-in version instead of what you build.

If you opt for deleting `bm-strategies.jar` - go to the `lib` folder inside bookmap installation folder (on Windows it's `C:\Program Files\Bookmap\lib` by default) and delete `bm-strategies.jar` (with bookmap closed). Then start bookmap. Built-in Chase/Escape/Execute strategies will disappear, and now you are free to modify any classes in this demo without the need to rename. Note that you don't have to do that if you intend to rename classes or write your own - it only matters if full class name matches an existing one exactly.

## Which API to chose?

If you are just starting - go for Simplified API. It's much easier, and at least you will be able to validate that everything is set up correctly before diving into the more complex core API. Don't be fooled by a name though - while it is a wrapper around core API intended to simplify it - it is still reasonably powerful.

Core API can do everything that simplified API does (because it's just a wrapper around core), but it is much more complicated. Not just because of having more functionality to deal with, but also because of it being less polished in general - it can do a lot, but some things aren't as clean as those could be. Typically it does provide you tools to achieve the same task more efficiently though.



# More detailed guide

## Some terminology confusion

Initially the API was built mainly for strategies, so you will see quite a few places in the code where module is referred to as "Strategy". Please disregard that and read it as "Module".

## General data flow

Bookmap L1 API in general is built around the following idea: there is a chain or layers, each one can do some processing on the data and then might forward it to the next one. It's similar to chain-of-responsibility pattern, but there are few distinctions:
- Data is forwarded further nearly always, meaning that multiple objects can (and usually will) process it.
- Data flows through the stack in both directions. When something happens in the market (one of the levels changes, or your order gets executed) event goes up the stack. When user does something (requests a subscription to an instrument, sends an order) event travels down the stack.
- Stack can branch at some points. See the part about attached modules for more details.

On the bottom of the stack are the providers. Those receive the data from an outside world and feed it into Bookmap through the stack. On the top of the stack GUI is located.

External modules, as you have probably realized by now, are loaded into the middle of the stack. Depending on the mode (Injected/Attached/Data editor) exact placement will vary.

## Syncronization rules

Bookmap relies on one very important assumption: upstream and downstream events happen independently. This means that you must never synchronize those two streams, e.g. by waiting for certain downstream event to complete in upstream method or the other way around.

`Layer1ApiInjectorRelay#inject` is helpful if you need to inject some upstream events from a downstream method.

## Main API components

There are few "parts" of L1 API that are worth knowing about:
- Simplified wrapper - great way to create something quickly
- Core - lets you process events that pass through your module, realtime only
- Data structure interface - you can ask bookmap to extract recent events from a built-in storage
- Generators extension - if built-in data structure events aren't good enough you can add your own
- Settings storage - lets you store your settings inside bookmap config/workspace files
- Custom panels - lets the user configure your module via a panel in the built-in dialog
- Indicators extension - lets you draw lines and place icons on top of data
- Screen space painter extension - similar to indicators extension in terms of what it does, but isn't limited to just icons and lines - overlay arbitrary images on top of heatmap area

### Simplified wrapper

It's a built-in wrapper that is actually using many other parts of the API. It provides you with a simple way to implement most indicators and some strategies without having to worry about all the complexities of Core API. You'll still face some concepts from the Core API, but only a small portion.

Events are fed to your module in the same order as those have happened and you place dots or icons using price-time coordinates.

3 "modes" of operation are supported. Mode will be picked automatically based on interfaces you implement:
- Live only. You get a snapshot when loaded, and then realtime updates are sent.
- Live+history with mode notification (`HistoricalModeListener`). You get all the events since bookmap started. After you get all historical events you get a notification about switching to realtime data.
- Live+history without mode notification (`HistoricalDataListener`). Similar to the previous one, but you don't get a notification when data switches to realtime, so it isn't suitable for strategies.

Points that you generate can be stored in one of 2 ways:
- In-memory. Events are stored in memory and can be edited quickly (you can rewrite history). Number of points is limited, older ones start being deleted when there are too many, around 200000 (we avoid deleting all events, but we thin out the older ones, which will decrease the resolution of older data; this is done to prevent excessive memory usage).
- On disk. That removes the limitation on quantity. Rewriting history isn't supported. This is only possible in "Live+history without mode notification" mode and only if you register a non-modifiable indicator - otherwise in-memory mode will be used automatically.

Most of this documentation isn't relevant if you are only interested in the Simplified API only, so feel free to skip it.

### Core

It includes commonly used types (like `InstrumentInfo`) and general concepts like "layered" approach described in the data flow part.

Each externally loaded layer has a lifecycle and must only send out requests/events when it's active. Keep in mind that it doesn't become active immediately in the constructor - you get a separate notification (`UserMessageLayersChainCreatedTargeted`). Also note that layer can be reused (i.e. made inactive and then active again). Every time the layer is made active it will get some snapshot-like data that provides general information about current state (like which instruments user is subscribed to and what are the order books for those).

### Data structure interface

Use `Layer1ApiDataInterfaceRequestMessage` to retrieve data `DataStructureInterface` object. After that you can request data for arbitrary intervals with certain aggregation level. As long as the number of samples isn't too high (less than a few thousands) it shouldn't take too long. This way of getting data is much faster than iterating through raw events on larger intervals. You can still use it even when interval is small (and it will be reasonably fast), but it technically will be slower than simply iterating through events, if you store those somewhere.

Every aggregation (that describes certain interval) is equivalent to applying corresponding updates sequentially. Do not confuse it with snapshots. E.g. `DepthAggregationEvent`, which can be part of aggregation if requested, will contain information about all levels that changed within the interval, but will not contain intermediate updates (e.g. if same level changed few times - you'll only get the last one, because depth updates define a new size on the level and replace the previous update).

Some methods of `DataStructureInterface` return N+1 elements when you request N intervals (see Javadoc). In this case 0th element contains an aggregation from time=0 till the left edge of the leftmost interval - this is essentially the snapshot, which is represented as an aggregation. This allows you to populate your structures and perform computations with a single request. E.g. if you want to compute some sort of mid price you would populate your order book using 0th element and then would be able to compute mid price in N key points by sequentially applying incremental updates from N aggregations.

### Generators extension

Sometimes you might want to insert custom events so you can later retrieve those from data structure (in the same mid price example it would be more efficient to compute mid price in advance once and store it as points instead of having to recompute it every time from a heavyweight depth data).

_Note that it's fully supported only in 7.1.0 b25 and above. In older builds you have to do some tricks with placing your objects into root classloader in order to make it work._

In order to use it you need to do a few things:
- First you need to define your value class (it describes your points) and aggregation class (it describes an aggregation of your points over certain range).
- After that you need to define your aggregator. This interface has few methods allowing you to define how individual points are transformed into aggregations and how multiple aggregations can be merged. In other words, you define which information should be saved and which should be discarded when aggregation happens.
- Than you can register your generator. Send `Layer1ApiUserMessageAddStrategyUpdateGenerator` to do that. It will start being calculated shortly after you send the message.

After generator has finished the calculations you will be able to retrieve those from data structure. You'll also start receiving realtime events in your listeners (inside `onUserMessage` you will see objects of `CustomGeneratedEventAliased`, and some of those will contain your value class inside).

### Settings storage

You module will typically have some settings you want to store. Mark your module with `Layer1ConfigSettingsInterface` and you'll get a `SettingsAccess` that allows you to store per-instrument settings or global settings (alias == null).

### Custom panels & controlling enabled checkbox

Mark your module with `Layer1CustomPanelsGetter` and bookmap will be calling `getCustomGuiFor` method when it needs you to create a UI. Note that it can happen even if your module isn't active according to lifecycle. This means your module isn't enabled and you'll typically want to return a fully disabled panel (so user sees a mockup of your UI, but can't really do anything).

Note that there is a checkbox near your module name. By default it will load/unload your module. However, you can intercept the control over that checkbox by implementing `Layer1ApiInstrumentSpecificEnabledStateProvider` interface. When this is done your module will always stay loaded (as long as it's added to Bookmap), and you can define how this checkbox behaves. Recommended way is that it should enable/disable your module individually for current instrument.

### Indicators extension

As a part of bookmap L1 api you can develop indicators that are displayed over the heatmap or in the panel below it. 
Indicators can be registered using `Layer1ApiUserMessageModifyIndicator` (you can use `Layer1ApiUserMessageModifyIndicator.Builder` to create it). There are many parameters controlling various aspects. Examples are the best place to get started (e.g. `Layer1ApiMarkersDemo`).

_Please note that right now style parameters (like line width) do not work for indicators displayed on bottom panel._

Indicator can show a line (bottom panel or over heatmap), icons (over heatmap only, in the nodes of an invisible line), and widget. Any combination of those is allowed. Keep in mind that line and icons are always in price/time coordinates - you can't use screen space coordinates with this API.

Single module can register multiple indicators.

The indicator is described by `OnlineCalculatable` interface which contains two important methods:
- `calculateValuesInRange` - this one will be called when your indicator needs data for a specific time range. Usually it happens when user moves heatmap around. This method does not necessarily have to be very fast. In this method you will usually request aggregated data from a storage and process it.
- `createOnlineValueCalculator` - this one is intended for calculating indicator values in real time. You will have to provide a calculator that will react on incremental updates recomputing the current value as necessary.

Note that you can invalidate your indicator (trigering `calculateValuesInRange` and then `createOnlineValueCalculator`) at any point if, for example, settings have changed. Or you can just publish new values from your online value calculator using `IndicatorFullValues`.


### Screen space painter extension

_This extension was added in 7.1_

This is a tool that can be used if you want to draw arbitrary information on top of heatmap. You can draw in both pixel and price/time coordinates, and you can also mix those (e.g. "10 pixels to the right from 16:00:00").

Unique feature of this extension is an ability to render elements that are more complicated than lines or which are expected to have static position (e.g. an overlay showing a probability that something happens).

In certain cases it can replace Indicators extension reasonably well, though it currently is limited to drawing images (meaning you would have to construct the line yourself, and doing that efficiently would require some tricks) and will require more low-level considerations in regards to how you compute things (Indicators extension takes care of moving and deleting your points automatically as time moves, connecting slow range-based computations and fast realtime ones, etc.)

## Module types

Most API parts can be mixed freely. However there are some choices that you have to make depending on what you are trying to achieve that will impact which functionality is available to you.

### Simplified module

This is a wrapper around Attached module. These modules can't use most general-purpose functionality, like generators, and have an alternative functionality replacing those.

### Attached module

Attached modules (marked with `@Layer1Attachable`) don't really get inserted into the middle of the stack. Instead those are attached to a certain "attachment point" in the middle of the stack. This is more efficient than injecting as module is not required to forward the messages. Unless you have some special requirements - this is the mode you should use.

The main limitation you have to consider is that downstream events (like user sending an order) will not pass through your module. The effect of those downstream events (like message from a provider acknowledging the order being sent) however will, since those are actually upstream events.

### Injected module

Injected modules (marked with `@Layer1Injectable`) are placed between certain stack layers. This means that you must forward all upstream and downstream events, unless you want those to be lost.
Keep in mind that ignoring or modifying upstream events in injected module will often have unwanted side effects due to layer placement - it will be located above the data storage, meaning that those changes won't be persisted; you should use Data editor modules if that's your goal.

Injected modules can do everything attached modules can do. The main benefits are:
- ability to alter downstream events (e.g. reject submission of a risky order, or maybe ask for additional confirmation)
- control over the exact time when an event will continue going upstream/downstream (before or after your code being executed).

However it has a slight performance cost, so use Attached module if you don't need to utilize Injected module features.

You might want to extend `Layer1ApiRelay` or `Layer1ApiInjectorRelay` when creating such module.

### Data editor module

_This mode was added in 7.1_

Data editor modules (marked with `@Layer1UpstreamDataEditor`) are specialized modules that can rewrite upstream events. You can create synthetic instruments, process data in some way (e.g. filter sizes), etc. 

It's important to keep in mind that such module **must** return whatever it has changed to initial state when being unloaded. If you created some instruments - delete those, if you filtered out some levels - bring those back. See `DataEditorBasicExample` for an example.

Data editors are placed below the data structure, meaning that all the changes will be persisted correctly. However this also prevents such modules from utilizing most of extensions normally available. Settings storage, custom panels and controlling enabled checkbox should still be available.

You might want to extend `Layer1ApiRelay` or `Layer1ApiInjectorRelay` when creating such module.

## Performance considerations

Near the lowest level of the stack (adapters) there is a layer assigning timestamps and queuing all events. Past that point delays do not change event timestamp - instead those "slow down time", meaning that timeline in Bookmap will start moving slower if bookmap doesn't keep up. It isn't a normal state, but can happen during data bursts if one of the loaded modules is too slow to keep up. Bookmap will try to catch up (including temporarily suspending heatmap rebuild) and, in the case of short-term burst, will recover without losing data as soon as it processes queued events.

In order to avoid that you should consider that the following code is executed inside the main stack. This means that if it's slow - it will slow down the data processing.
- Generators start being executed in the main stack as soon as "historical" part of data is processed.
- `OnlineValueCalculatorAdapter` returned by `OnlineCalculatable#createOnlineValueCalculator` is executed inside the main stack as soon as it catches up with data (it gets a catch up portion of updates asynchronously right after being created and then is attached to the main stack)

## Licensing

It you'd like to utilize Bookmap licensing you can mark your module with `Layer1StrategyDateLicensed`. Field name should be received from Bookmap first. If user doesn't have this field set or it has expired - Bookmap will refuse to load your module (`LargeOrdersFilter` demo utilizes this, though "BmDemo-LargeOrdersFilter" field is set for everyone).

## Limitations

Note that due to limitations imposed by providers you might be unable to receive data for certain data sources. In this case API will mostly behave like if instruments received from those data sources do not exist.

## Gradle artifacts, Javadoc

As you can see in `build.gradle`, additional repository is added and following artifacts are included:
```
        compileOnly group: 'com.bookmap.api', name: 'api-core', version: 'x.y.z.b';
        compileOnly group: 'com.bookmap.api', name: 'api-simplified', version: 'x.y.z.b';
```
`x.y.z` part is Bookmap version for which module is compiled and `b` is the specific build (see next section for information about compatibility). You can switch to a newer version/build to gain access to newer functionality.
To avoid conflicts with dependencies Bookmap is using please view **Classloaders, compatibility** section.

Note that repository also contains javadoc, and while it's far from perfect it still recommended to make sure you download it and read it before using corresponding functionality.

## IDE and tricks

You should be able to use any IDE you like as long as it supports Gradle. The process was validated with Eclipse, but it is not the only option (and, as shown earlier - you don't even really need an IDE). You might have to add and configure certain gradle plugins though, depending on the IDE you chose. In Eclipse you just need to import gradle project from `Strategies` directory using Buildship plugin.

You might also want to run Bookmap from IDE. This will make development faster and also allow you to attach debugger to Bookmap and debug your code. In order to do this:
- Use 64 bit Java 8 VM
- Sometimes your IDE may ignore your gradle source/target compatibility settings for Java (if you have those). Ensure it is Java 8 in your project environment/compiler settings. If not, set it explicitly in the project settings.
- Sometimes your IDE may handle your gradle compileOnly dependencies incorrectly and still add those to classpath. **This will usually result in NoSuchMethodError or NoClassDefFoundError**. You can verify that this is the case by checking `java.class.path` system properly of the process that you started via Java VisualVM on in any other way - it shouldn't contain any of compileOnly dependencies, so if it does - this is a problem. To solve it you can either (hackish simple way) edit build.gradle to point to the libraries from `C:\Program Files\Bookmap\lib` directory (which will ensure that those are the same exact libraries that bookmap expects, preventing the crash) or (more correct way, but exact way to achieve it differs from one IDE to another) remove the project and project dependencies from run configuration.
- Working directory will determine where your config folder will be. Note that Bookmap will create some folders next to it (not within it). On Windows you can set `C:\Bookmap\Config`, which is the default during installation, but you can also maintain multiple separate Bookmap configs, if you want.
- Add `C:\Program Files\Bookmap\Bookmap.jar` to the classpath. It should list the dependencies in manifest, so that will often be enough, but you can include libraries from `C:\Program Files\Bookmap\lib` if Bookmap complains about missing classes.
- Start velox.ib.Main

Now you should be able to start bookmap from IDE directly. 

You might want to avoid packing jar every time you want to make changes. Best way to do it is the following:
- create empty `bm-strategy-package-fs-root.jar` file in a folder where your class files are. E.g. it could be something like `Strategies\build\classes\java\main` (though it will usually be different depending on your IDE). This folder will be passed to URLClassLoader, so it should contain folder(s) matching top level package names, which contain either other folders corresponding to lower level package names or class files, e.g. `HelperStrategySettings.class`.
- load this file into bookmap as you would normally load your jar file. When empty file named `bm-strategy-package-fs-root.jar` is loaded, bookmap will not actually read the file but instead will load classes from a folder where it is located.
- file can be deleted after it's loaded into bookmap. Bookmap only relies on the file in file dialog and no longer needs it afterwards.
- keep in mind that you should use a folder where your class files are actually recompiled to. E.g. you might have 2 folders (one used by gradle and another used by IDE - you probably should use the one used by IDE, as it will save you from invoking gradle after every change as long as IDE recompiles classes automatically)

If you set Bookmap to load your module every time you can simply restart Bookmap after making changes or even edit the code without restart if debugger is attached.

## How your module is loaded (Classloaders, compatibility)

Your module is loaded into a custom child classloader. It is a "normal" parent-first classloader, meaning that if there is the same class in both your jar and Bookmap - the one from Bookmap will be prefered.

You can ship necessary libraries with your code, and the easiest way to achieve this would be to create a "fat jar", also known as "uber". Just be aware of potential conflicts with the libraries that are packaged with Bookmap - e.g. it's probably not a great idea to try using a different version of the same library (it can be done in a way that will work, e.g. by using another classloader, just be aware of conflicts if you decide to do it in a simple way).

A common way to keep versions consistency for the libraries is taking a look into Bookmap's lib folder and checking libraries versions. Explicitly make gradle use the versions of libraries Bookmap uses.  
Example: gson library Bookmap uses is in the lib folder (`gson-2.8.5.jar`). You can find the notation for Gradle in the maven repository and add it to the dependencies in your project's gradle.build:  
`compileOnly group: 'com.google.code.gson', name: 'gson', version: '2.8.5'`

When new version of Bookmap is released it will usually be able to load the code created for older versions (but not the other way around).

Sometimes breaking changes are introduced into the api, and when that happens we usually try to adjust our compatibility layer to ensure that older code will remain functional. In order for your module to take advantage of that make sure to utilize `@Layer1ApiVersion` annotation. You should typically specify the latest API version available. When a version with breaking changes is released it will be able to recognize that your module relies on an older API and will try to ensure it still loads.

## Loading native code

In general it's not recommended, unless you have a very specific reason to do so. Keep in mind that it's harder to debug and that you will have to compile separate binaries for different operating systems. However, if you still decide to do so (e.g. to make your code harder to reverse), here are a few recommendations:

- Make sure you unload your native code when your module is unloaded.
- You might want to use JNA for loading your library. It is part of Bookmap installer, so you can include it as compileOnly dependency.

## Notification system

For details on notification system, take a look at the [Notification system README](./Strategies/src/main/java/velox/api/layer1/simpledemo/alerts/README.md)
