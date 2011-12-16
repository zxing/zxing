TEMPLATE = app
TARGET = QQrDecoder
QT += core \
    gui \
    declarative

VERSION = 1.2.1

CONFIG += mobility
MOBILITY = multimedia #\
    #systeminfo

RESOURCES +=  resources.qrc

HEADERS += QQrDecoder.h \
    QCameraControllerWidget.h \
    button.h \
    myvideosurface.h
SOURCES += main.cpp \
    QQrDecoder.cpp \
    QCameraControllerWidget.cpp \
    button.cpp \
    myvideosurface.cpp
FORMS += QQrDecoder.ui

symbian{
    TARGET.UID3 = 0xEF2CE79D
    TARGET.EPOCSTACKSIZE = 0x14000
    TARGET.EPOCHEAPSIZE = 0x20000 0x8000000

    # Because landscape orientation lock
    LIBS += -lcone -leikcore -lavkon -lqzxing

    # Self-signing capabilities
    TARGET.CAPABILITY += NetworkServices \
        ReadUserData \
        WriteUserData \
        LocalServices \
        UserEnvironment

    customrules.pkg_prerules  = \
        ";QZXing" \
        "@\"$$(EPOCROOT)Epoc32/InstallToDevice/QZXing_selfsigned.sis\",(0xE618743C)"\
        " "
    DEPLOYMENT += customrules
}

ICON = QQrDecoder.svg





