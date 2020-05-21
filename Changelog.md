# Rails-18xx 2.2 Release
This release is meant to show the current state of the Rails-18xx implementation.

Due to different reasons Rails-18xx 2.0 and 2.1 have not been release as production releases. We hope that the audience finds the release useful.

As usual we welcome bug reports and the saved games that show the affected states of the game.

We would like to **thank** the varios bug reporters and the developers who have contributed over the years for one feature or ground breaking work.


## New Features

* Notification of moves via Slack
* Notification of moves via Discord
* Auto-load/save and autopolling improvments for near real time gaming with shared drives/folders (notes [here](https://github.com/Rails-18xx/Rails/wiki/PlayingModes)
* Direct Revenue Allocation for various Contracts possible
   * State of Direct Revenue Allocation is static towards company operations budget (no Half pay) - Workaround you half the allocation and add the rest to normal revenue distribution
* Installers for Windows, Mac OS, Linux (RPM, Debian)

## New games
We continue on expanding the game base, the first new game on this for this release is 18Chesapeake.

## Status of Games
* Implementation of 18Chesapeake started, needs playtesting, bug reports welcome
* Implementation of 1837 - Have a look but be prepared that it might break... 
   * Open Topics in 1837 Implementation:
     * Revenue Calculation of Coal Trains
     * Coal Minors merging round implementation not always working as intended..

## Bug Fixes
* 1835 : 
  * Numerous Fixes in regard to the director shares (you cant split sell them in 1835)
  * Fixes for the Forced Sale of Shares 
* 1856:
  * Fix multiple Rights issue upon foundation of the CGR

## Issues fixed:
* [#55](https://github.com/Rails-18xx/Rails/issues/55)  Rails freezes when adding the same player name during game creation
 * #205 
 * #180
 * #179
* [#129](https://github.com/Rails-18xx/Rails/issues/129)  unable to dump presidency in 1835
* [#130](https://github.com/Rails-18xx/Rails/issues/130)  certificate exchange in 1835: how can we help?
 * #75 
* [#73](https://github.com/Rails-18xx/Rails/issues/73)  1830 - problem when no shares to sell at all in forced buy
* [#72](https://github.com/Rails-18xx/Rails/issues/72)  18EU hex reservation doesn't cater for closed minors
* [#71](https://github.com/Rails-18xx/Rails/issues/71)  1835 - change of PR director after discarding trains
* [#78](https://github.com/Rails-18xx/Rails/issues/78)  18eu unpromotable tiles
* [#77](https://github.com/Rails-18xx/Rails/issues/77)  1835 - director may not exchange 20% for 10% share when selling
* [#76](https://github.com/Rails-18xx/Rails/issues/76)  1835 - wrong player gets PD if the last action was a sell action
* [#90](https://github.com/Rails-18xx/Rails/issues/90)  Failed reload during 1830 auction B&O par setting

