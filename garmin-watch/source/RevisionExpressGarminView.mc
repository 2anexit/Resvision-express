using Toybox.Communications;
using Toybox.Graphics;
using Toybox.Lang;
using Toybox.System;
using Toybox.WatchUi;

class RevisionExpressGarminView extends WatchUi.View {

    var category = "Revision Express";
    var question = "En attente d'une question...";
    var options = [ "Choix 1", "Choix 2", "Choix 3", "Choix 4" ];
    var questionIndex = 0;
    var totalQuestions = 20;
    var timerText = "01:00";
    var selectedIndex = 0;
    var locked = false;
    var feedback = "";
    var resultText = "";
    var screenMode = "idle";

    function initialize() {
        View.initialize();
        Communications.registerForPhoneAppMessages(method(:onPhoneMessage));

        if (Communications has :registerForPhoneAppMessageErrors) {
            Communications.registerForPhoneAppMessageErrors(method(:onPhoneMessageError));
        }
    }

    function onShow() as Void {
        WatchUi.requestUpdate();
    }

    function onHide() as Void {
        Communications.registerForPhoneAppMessages(null);
    }

    function onUpdate(dc as Toybox.Graphics.Dc) as Void {
        var width = dc.getWidth();
        var y = 10;

        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();

        dc.drawText(width / 2, y, Graphics.FONT_SMALL, category, Graphics.TEXT_JUSTIFY_CENTER);
        y += 24;

        if (screenMode == "result") {
            dc.drawText(width / 2, y, Graphics.FONT_MEDIUM, "Resultat", Graphics.TEXT_JUSTIFY_CENTER);
            y += 28;
            dc.drawText(width / 2, y, Graphics.FONT_SMALL, resultText, Graphics.TEXT_JUSTIFY_CENTER);
            return;
        }

        dc.drawText(width / 2, y, Graphics.FONT_XTINY, timerText, Graphics.TEXT_JUSTIFY_CENTER);
        y += 18;
        dc.drawText(width / 2, y, Graphics.FONT_XTINY, Lang.format("$1$ / $2$", [ questionIndex + 1, totalQuestions ]), Graphics.TEXT_JUSTIFY_CENTER);
        y += 22;

        dc.drawText(12, y, Graphics.FONT_SMALL, question, 0);
        y += 54;

        for (var i = 0; i < options.size(); i += 1) {
            var prefix = (i == selectedIndex) ? "> " : "  ";
            dc.drawText(12, y, Graphics.FONT_XTINY, prefix + options[i], 0);
            y += 22;
        }

        if (feedback != null && feedback.length() > 0) {
            y += 8;
            dc.drawText(12, y, Graphics.FONT_XTINY, feedback, 0);
        }
    }

    function moveSelection(step as Toybox.Lang.Number) as Void {
        if (locked) {
            return;
        }

        selectedIndex = selectedIndex + step;
        if (selectedIndex < 0) {
            selectedIndex = options.size() - 1;
        } else if (selectedIndex >= options.size()) {
            selectedIndex = 0;
        }

        WatchUi.requestUpdate();
    }

    function submitSelection() as Void {
        if (locked) {
            return;
        }

        var payload = {
            "type" => "answer",
            "questionIndex" => questionIndex,
            "answerIndex" => selectedIndex
        };

        Communications.transmit(payload, {}, new RevisionExpressTransmitListener());
    }

    function onPhoneMessage(msg as Communications.PhoneAppMessage) as Void {
        var data = msg.data;
        if (data == null) {
            return;
        }

        var messageType = data["type"];
        if (messageType == "idle") {
            screenMode = "idle";
            category = "Revision Express";
            question = "Lance une serie sur le telephone.";
            feedback = "";
            locked = false;
            WatchUi.requestUpdate();
            return;
        }

        if (messageType == "result") {
            screenMode = "result";
            category = data["category"];
            resultText = Lang.format("Score $1$ / $2$", [ data["score"], data["totalQuestions"] ]);
            if (data["timedOut"] == true) {
                resultText += "\nTemps ecoule";
            }
            WatchUi.requestUpdate();
            return;
        }

        screenMode = "question";
        category = data["category"];
        question = data["question"];
        questionIndex = data["questionIndex"];
        totalQuestions = data["totalQuestions"];
        options = [ data["option1"], data["option2"], data["option3"], data["option4"] ];
        timerText = formatTimer(data["remainingMillis"]);
        feedback = data["feedback"];
        locked = data["answerLocked"];

        var incomingSelectedIndex = data["selectedIndex"];
        if (incomingSelectedIndex != null && incomingSelectedIndex >= 0) {
            selectedIndex = incomingSelectedIndex;
        } else {
            selectedIndex = 0;
        }

        WatchUi.requestUpdate();
    }

    function onPhoneMessageError(err as Communications.PhoneAppMessageError) as Void {
        System.println(Lang.format("Phone message error: $1$", [ err ]));
    }

    function formatTimer(remainingMillis as Toybox.Lang.Number) as Toybox.Lang.String {
        var totalSeconds = remainingMillis / 1000;
        var minutes = totalSeconds / 60;
        var seconds = totalSeconds % 60;
        return Lang.format("$1$:$2$", [ pad(minutes), pad(seconds) ]);
    }

    function pad(value as Toybox.Lang.Number) as Toybox.Lang.String {
        if (value < 10) {
            return "0" + value.toString();
        }
        return value.toString();
    }
}

class RevisionExpressGarminDelegate extends WatchUi.BehaviorDelegate {

    var view;

    function initialize(appView as RevisionExpressGarminView) {
        BehaviorDelegate.initialize();
        view = appView;
    }

    function onNextPage() as Toybox.Lang.Boolean {
        view.moveSelection(1);
        return true;
    }

    function onPreviousPage() as Toybox.Lang.Boolean {
        view.moveSelection(-1);
        return true;
    }

    function onSelect() as Toybox.Lang.Boolean {
        view.submitSelection();
        return true;
    }
}

class RevisionExpressTransmitListener extends Communications.ConnectionListener {

    function initialize() {
        ConnectionListener.initialize();
    }

    function onComplete() as Void {
    }

    function onError() as Void {
    }
}
