import QtQuick 1.1
import com.nokia.symbian 1.1

CommonDialog {
    id: dialog
    titleText: "Decoded message"
    //titleIcon: "images/QrButton_toolbar.svg"
    buttonTexts: ["OK"]

    property alias text: messageLabel.text

    content: Rectangle {
        height: messageLabel.height
        width: messageLabel.width
        color: "#ff000000"
        anchors.horizontalCenter: parent.horizontalCenter

        Text{
            id: messageLabel
            color: "white"
        }
    }
}
