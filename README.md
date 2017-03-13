# DemoStrategies
The project should help you get started with Bookmap Layer 1 API

## Getting started
A short environment setup guide is available here
[Getting started + overview.pdf](Doc/Getting started + overview.pdf)
Also after you install Bookmap javadoc will be available in C:\Program Files (x86)\Bookmap\lib\bm-l1api-javadoc.jar (or C:\Program Files\Bookmap\lib\bm-l1api-javadoc.jar for x64 version).

## API changes

### 17.02.2017: Manual price range calculations
Added Layer1PriceAxisRangeCalculatable interface that can be used to control price range (Y axis range) and price lables for strategy indicators. Added PriceRangeCalculationHelper helper class for standart "good numbers" implementation.

### 24.01.2017: IndicatorDisplayLogic.setTooltipFormatter renamed to setValuesFormatter
Method name did not fully match what it actually did before. Just rename it in your code - functionality is unchanged.

### 13.01.2017: Strategies naming changed
Earlier to generate indicator you would provide fullName and userName to message, and fullName would be used by system to identify your indicator (like online indicator caclulations).
Now Layer1ApiUserMessageModifyIndicator constructor accepts owner class name and userName (that will be used to show indicator name in different places). Full name is generated as combination of those, and will
be unique (unless there is 2 indicators with same owner class and same user names). It can be accessed after message is created from fullName member. You should remember this generated name as it will be used
to make any requests for your indicator.

### 13.01.2017: Layer1CustomPanelsGetter#getCustomGuiFor
Now also accepts indicatorName. If strategy was selected via settings button of strategies configuration popup this will be the name of indicator (useful if you have more then 1 indicator created by strategy,
and want to show this indicators settings for example)
