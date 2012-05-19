TEMPLATE=app

VERSION = 1.1.0

QT += declarative network

!maemo5 {
    contains(QT_CONFIG, opengl) {
       # QT += opengl
    }
}

win32 {
    #required by Qt SDK to resolve Mobility libraries
    CONFIG+=mobility
    MOBILITY+=multimedia
}

SOURCES += $$PWD/qmlcamera.cpp
!mac:TARGET = QMLBarcodeReader
else:TARGET = QMLBarcodeReader

RESOURCES += declarative-camera.qrc

symbian {
    TARGET.CAPABILITY = UserEnvironment NetworkServices Location ReadUserData WriteUserData
    TARGET.EPOCHEAPSIZE = 0x20000 0x3000000

    LIBS += -lqzxing

    customrules.pkg_prerules  = \
        ";QZXing" \
        "@\"$$(EPOCROOT)Epoc32/InstallToDevice/QZXing_selfsigned.sis\",(0xE618743C)"\
        " "
    DEPLOYMENT += customrules
}

ICON = QMLBarcodeReader.svg
