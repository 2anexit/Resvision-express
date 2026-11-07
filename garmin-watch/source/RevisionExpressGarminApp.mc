using Toybox.Application;
using Toybox.WatchUi;

class RevisionExpressGarminApp extends Application.AppBase {

    function initialize() {
        AppBase.initialize();
    }

    function getInitialView() {
        var view = new RevisionExpressGarminView();
        var delegate = new RevisionExpressGarminDelegate(view);
        return [ view, delegate ];
    }
}

function getApp() as RevisionExpressGarminApp {
    return Application.getApp() as RevisionExpressGarminApp;
}
