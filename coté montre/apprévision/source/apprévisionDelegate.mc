import Toybox.Lang;
import Toybox.WatchUi;

class apprévisionDelegate extends WatchUi.BehaviorDelegate {

    function initialize() {
        BehaviorDelegate.initialize();
    }

    function onMenu() as Boolean {
        WatchUi.pushView(new Rez.Menus.MainMenu(), new apprévisionMenuDelegate(), WatchUi.SLIDE_UP);
        return true;
    }

}