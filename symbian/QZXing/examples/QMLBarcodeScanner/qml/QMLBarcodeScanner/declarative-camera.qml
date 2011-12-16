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
import QZXing 1.0

Rectangle {
    id : cameraUI
    color: "black"

    Camera {
        id: camera
        x: 0
        y: 0
        width: parent.width - stillControls.buttonsPanelWidth
        height: parent.height
        focus: visible //to receive focus and capture key events
        captureResolution : "640x480"

        flashMode: stillControls.flashMode
        whiteBalanceMode: stillControls.whiteBalance
        exposureCompensation: stillControls.exposureCompensation

        onImageCaptured : {
            decoder.decodeImage(preview);
        }
    }

    CaptureControls {
        id: stillControls
        anchors.fill: parent
        camera: camera
    }

    QZXing{
        id: decoder
        onTagFound: {
            messageBox.setText(tag)
            messageBox.state = "visible"
        }

        function enableQrCodeAndEAN()
        {
            setDecoder(DecoderFormat_QR_CODE | DecoderFormat_EAN_13);
        }
    }

    Rectangle{
        id: messageBox
        anchors.left: cameraUI.left
        anchors.leftMargin: 10
        anchors.right: cameraUI.right
        anchors.rightMargin: 10
        y: cameraUI.height / 4
        height: cameraUI.height/2

        state: "hidden"

        function setText(str)
        {
            textArea.text = str;
        }

        Text{
            id: textArea
            anchors.centerIn: parent
        }

        Rectangle{
            border.width: 2
            width: tagLabel.width
            height: tagLabel.height
            anchors.bottom: messageBox.bottom
            anchors.bottomMargin: 5
            anchors.right: messageBox.right
            anchors.rightMargin: 5

            Text{
                id: tagLabel
                text: "close"
            }
            MouseArea{
                anchors.fill: parent
                onClicked: messageBox.state = "hidden"
            }
        }

        states:[
            State{
                name: "visible"
                PropertyChanges {
                    target: messageBox
                    opacity: 1
                }
            },
            State{
                name: "hidden"
                PropertyChanges {
                    target: messageBox
                    opacity: 0
                }
            }
        ]
        Behavior on opacity{NumberAnimation{duration:100}}
    }
}
