/****************************************************************************
**
** Copyright (C) 2010 Nokia Corporation and/or its subsidiary(-ies).
** All rights reserved.
** Contact: Nokia Corporation (qt-info@nokia.com)
**
** This file is part of the examples of the Qt Mobility Components.
**
** $QT_BEGIN_LICENSE:BSD$
** You may use this file under the terms of the BSD license as follows:
**
** "Redistribution and use in source and binary forms, with or without
** modification, are permitted provided that the following conditions are
** met:
**   * Redistributions of source code must retain the above copyright
**     notice, this list of conditions and the following disclaimer.
**   * Redistributions in binary form must reproduce the above copyright
**     notice, this list of conditions and the following disclaimer in
**     the documentation and/or other materials provided with the
**     distribution.
**   * Neither the name of Nokia Corporation and its Subsidiary(-ies) nor
**     the names of its contributors may be used to endorse or promote
**     products derived from this software without specific prior written
**     permission.
**
** THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
** "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
** LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
** A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
** OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
** SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
** LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
** DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
** THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
** (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
** OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
** $QT_END_LICENSE$
**
****************************************************************************/

import Qt 4.7
import QtMultimediaKit 1.1

Item {
    id: propertyButton
    property alias value : popup.currentValue
    property alias model : popup.model

    width : 144
    height: 70

    BorderImage {
        id: buttonImage
        source: "images/toolbutton.sci"
        width: propertyButton.width; height: propertyButton.height
    }

    CameraButton {
        anchors.fill: parent
        Image {
            anchors.centerIn: parent
            source: popup.currentItem.icon
        }

        onClicked: popup.toggle()
    }

    CameraPropertyPopup {
        id: popup
        anchors.right: parent.left
        anchors.rightMargin: 16
        anchors.top: parent.top
        state: "invisible"
        visible: opacity > 0

        currentValue: propertyButton.value

        states: [
            State {
                name: "invisible"
                PropertyChanges { target: popup; opacity: 0 }
                PropertyChanges { target: camera; focus: true }
            },

            State {
                name: "visible"
                PropertyChanges { target: popup; opacity: 1.0 }
            }
        ]

        transitions: Transition {
            NumberAnimation { properties: "opacity"; duration: 100 }
        }

        function toggle() {
            if (state == "visible")
                state = "invisible";
            else
                state = "visible";
        }

        onSelected: {
            popup.state = "invisible"
        }
    }
}

