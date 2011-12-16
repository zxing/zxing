#include <QtGui/QApplication>
#include <QZXing.h>
#include "qmlapplicationviewer.h"

int main(int argc, char *argv[])
{
    QScopedPointer<QApplication> app(createApplication(argc, argv));
    QScopedPointer<QmlApplicationViewer> viewer(QmlApplicationViewer::create());

    QZXing::registerQMLTypes();

    viewer->setOrientation(QmlApplicationViewer::ScreenOrientationLockPortrait);
    viewer->setMainQmlFile(QLatin1String("qml/QMLBarcodeScanner/declarative-camera.qml"));
    viewer->showExpanded();

    return app->exec();
}
