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

Item {
    id: flickableList
    clip: true

    signal clicked

    property alias delegate : repeater.delegate

    property variant items: []
    property int index: 0
    property int itemWidth : flickableList.width

    function scrollTo(id) {
        var x = id*flickableList.itemWidth
        if (flickArea.contentX != x) {
            centeringAnimation.stop();
            flickArea.newX = id*flickableList.itemWidth
            centeringAnimation.start();
        }
    }

    onIndexChanged: scrollTo(index)
    onWidthChanged: scrollTo(index)

    Flickable {
        id: flickArea
        property int newX: 0

        MouseArea {
            anchors.fill: parent
            onClicked: {
                var x = mapToItem(flickableList, mouseX, mouseY).x

                if (x < flickableList.width/3) {
                    if (flickableList.index > 0)
                        flickableList.scrollTo(flickableList.index-1);
                } else if (x > flickableList.width*2/3) {
                    if (flickableList.index < flickableList.items.length-1)
                        flickableList.scrollTo(flickableList.index+1);
                } else {
                    flickableList.clicked()
                }

            }
        }

        PropertyAnimation {
            id: centeringAnimation
            target: flickArea
            properties: "contentX"
            easing.type: Easing.OutQuad
            from: flickArea.contentX
            to: flickArea.newX

            onCompleted: {
                flickableList.index = flickArea.newX / flickableList.itemWidth
            }
        }

        onMovementStarted: {
            centeringAnimation.stop();
        }

        onMovementEnded: {
            var modulo = flickArea.contentX % flickableList.itemWidth;
            var offset = flickableList.itemWidth / 2;
            flickArea.newX = modulo < offset ? flickArea.contentX - modulo : flickArea.contentX + (flickableList.itemWidth - modulo);
            centeringAnimation.start();
        }


        width: flickableList.width
        height: flickableList.height
        contentWidth: items.width
        contentHeight: items.height
        flickDeceleration: 4000

        Row {
            id: items
            Repeater {
                id: repeater
                model: flickableList.items.length
            }
        }
    }
}
