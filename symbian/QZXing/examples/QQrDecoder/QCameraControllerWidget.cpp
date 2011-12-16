/****************************************************************************
**
** Copyright (C) 2011 Nokia Corporation and/or its subsidiary(-ies).
** All rights reserved.
** Contact: Nokia Corporation (qt-info@nokia.com)
**
** This file is part of the demonstration applications of the Qt Toolkit.
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

#include "QCameraControllerWidget.h"
//#include <QDebug>
#include <QCameraFlashControl>


/*****************************************************************************
* QCameraControllerWidget
*/
QCameraControllerWidget::QCameraControllerWidget(QWidget *parent) :
    QWidget(parent)
{
    setWindowTitle("QCameraControllerWidget");

    // Opitimizations for screen update and drawing qwidget
    setAutoFillBackground(false);

    // Prevent to screensaver to activate
    //    m_systemScreenSaver = new QSystemScreenSaver(this);
    //    m_systemScreenSaver->setScreenSaverInhibit();

    m_myVideoSurface = 0;
    pictureCaptured = false;
    showViewFinder = false;
    m_focusing = false;

    // Black background
    QPalette palette = this->palette();
    palette.setColor(QPalette::Background, Qt::black);
    setPalette(palette);

    // Main widget & layout
    // QWidget* mainWidget = new QWidget(this);
    setPalette(palette);

    QHBoxLayout* hboxl = new QHBoxLayout;
    hboxl->setSpacing(0);
    hboxl->setMargin(0);

    // UI stack
    m_stackedWidget = new QStackedWidget();
    m_stackedWidget->setPalette(palette);

    // First widget to stack
    m_videoWidget = new QWidget();
    m_videoWidget->setPalette(palette);
    m_stackedWidget->addWidget(m_videoWidget);

    // Second widget to stack
    QWidget* secondWidget = new QWidget(this);
    secondWidget->setPalette(palette);
    m_stackedWidget->addWidget(secondWidget);
    m_stackedWidget->setCurrentIndex(0);


    // Buttons
    QSize iconSize(80, 80);
    QVBoxLayout* vboxl = new QVBoxLayout;
    vboxl->setSpacing(0);
    vboxl->setMargin(0);

    zoomIn = new Button(this);
    QObject::connect(zoomIn, SIGNAL(pressed()), this, SLOT(onZoomIn()));
    QPixmap p = QPixmap(":/icons/zoomIn");
    zoomIn->setPixmap(p.scaled(iconSize, Qt::KeepAspectRatio, Qt::SmoothTransformation));
    vboxl->addWidget(zoomIn);
    vboxl->setAlignment(zoomIn,Qt::AlignTop | Qt::AlignHCenter);

    captButton = new Button(this);
    QObject::connect(captButton, SIGNAL(pressed()), this, SLOT(searchAndLock()));
    p = QPixmap(":/icons/camera");
    captButton->setPixmap(p.scaled(iconSize, Qt::KeepAspectRatio, Qt::SmoothTransformation));
    vboxl->addWidget(captButton);
    vboxl->setAlignment(captButton,  Qt::AlignHCenter);

    zoomOut = new Button(this);
    QObject::connect(zoomOut, SIGNAL(pressed()), this, SLOT(onZoomOut()));
    p = QPixmap(":/icons/zoomOut");
    zoomOut->setPixmap(p.scaled(iconSize, Qt::KeepAspectRatio, Qt::SmoothTransformation));
    vboxl->addWidget(zoomOut);
    vboxl->setAlignment(zoomOut, Qt::AlignBottom | Qt::AlignHCenter);

    //hboxl->addLayout(vboxl);
    hboxl->addWidget(m_stackedWidget);
    hboxl->addLayout(vboxl);

    setLayout(hboxl);

    fileWatcher = new QFileSystemWatcher();
    fileWatcher->addPath(QDesktopServices::storageLocation(QDesktopServices::PicturesLocation));
    connect(fileWatcher, SIGNAL( directoryChanged (const QString &)), this, SLOT(deleteImage(const QString &)));

    imageFolder = QDir(QDesktopServices::storageLocation(QDesktopServices::PicturesLocation));
    QStringList filters;
    filters << "camera*";
    imageFolder.setNameFilters(filters);

    // Enable camera after 1s, so that the application is started
    // and widget is created to landscape orientation
    QTimer::singleShot(1000,this,SLOT(enableCamera()));
}

QCameraControllerWidget::~QCameraControllerWidget()
{
    if (m_myVideoSurface)
        m_myVideoSurface->stop();
    m_camera->stop();
    delete m_stackedWidget;
    delete m_stillImageCapture;
    delete m_camera;
    delete fileWatcher;
}


void QCameraControllerWidget::enableCamera()
{
    m_camera = new QCamera();
    m_camera->setCaptureMode(QCamera::CaptureStillImage);
    connect(m_camera, SIGNAL(error(QCamera::Error)), this, SLOT(error(QCamera::Error)));
    connect(m_camera, SIGNAL(lockStatusChanged(QCamera::LockStatus,QCamera::LockChangeReason)), this, SLOT(lockStatusChanged(QCamera::LockStatus,QCamera::LockChangeReason)));
    connect(m_camera, SIGNAL(stateChanged( QCamera::State) ), this, SLOT(onStateChanged(QCamera::State)));

    // Own video output drawing that shows camera view finder pictures
    //! [0]
    QMediaService* ms = m_camera->service();
    QVideoRendererControl* vrc = ms->requestControl<QVideoRendererControl*>();
    m_myVideoSurface = new MyVideoSurface(this,this,this);
    vrc->setSurface(m_myVideoSurface);

    connect(m_myVideoSurface, SIGNAL(imageCaptured(QImage)), this, SLOT(redirectImageSignalFromVideoFinder(QImage)));

    //! [0]
    // Image capturer
    m_stillImageCapture = new QCameraImageCapture(m_camera);
    connect(m_stillImageCapture, SIGNAL(imageCaptured(int,QImage)), this, SLOT(onImageCaptured(int,QImage)));

    // Start camera
    if (m_camera->state() == QCamera::ActiveState) {
        m_camera->stop();
    }
    m_videoWidget->show();
    m_camera->start();
    showViewFinder = true;

    cameraFocus = m_camera->focus();
}

void QCameraControllerWidget::mousePressEvent(QMouseEvent *event)
{
    QWidget::mousePressEvent(event);

    if (pictureCaptured) {
        // Starting view finder
        pictureCaptured = false;
        m_stackedWidget->setCurrentIndex(0);
        if (m_myVideoSurface) {
            showViewFinder = true;
        }
    }
}

void QCameraControllerWidget::searchAndLock()
{
    m_focusing = false;
    m_focusMessage.clear();

    if (pictureCaptured) {
        // Starting view finder again
        pictureCaptured = false;
        m_stackedWidget->setCurrentIndex(0);
        if (m_myVideoSurface) {
            showViewFinder = true;
        }
    }
    else {
        // Search and lock picture (=focus)
        if (m_camera->supportedLocks() & QCamera::LockFocus) {
            m_focusing = true;
            m_focusMessage = "Focusing...";
            m_camera->searchAndLock(QCamera::LockFocus);
        } else {
            // No focus functionality, take picture right away
            captureImage();
        }
    }
}

void QCameraControllerWidget::lockStatusChanged(QCamera::LockStatus status, QCamera::LockChangeReason reason)
{
    if (status == QCamera::Locked) {
        if (reason == QCamera::LockAcquired) {
            // Focus locked
            m_focusMessage.clear();
            m_focusing = false;
            // Capture new image
            captureImage();
            // Unlock camera
            m_camera->unlock();
        } else {
            if (m_focusing)
                m_focusMessage = "No focus, try again";
        }
    } else if (status == QCamera::Unlocked && m_focusing) {
        m_focusMessage = "No focus, try again";
    }
}

void QCameraControllerWidget::captureImage()
{
    if (pictureCaptured) {
        // Starting view finder again
        pictureCaptured = false;
        m_stackedWidget->setCurrentIndex(0);
        showViewFinder = true;
    }
    else {
        // Capturing image
        showViewFinder = false;
        // Get picture location where to store captured images
//        QString path(QDesktopServices::storageLocation(QDesktopServices::PicturesLocation));
//        QDir dir(path);

        // Get next filename
//        QStringList files = dir.entryList(QStringList() << "camera");//_*.jpg");
//        int lastImage = 0;
//        foreach ( QString fileName, files ) {
//            int imgNumber = fileName.mid(7, fileName.size() - 11).toInt();
//            lastImage = qMax(lastImage, imgNumber);
//        }
        // Capture image
        if (m_stillImageCapture->isReadyForCapture()) {
            m_imageName = QString("camera_0");//%1.jpg").arg(lastImage+1);
            m_stillImageCapture->capture(m_imageName);
        }
    }

    m_camera->unlock();
}

void QCameraControllerWidget::onImageCaptured(int id, const QImage &preview)
{
    m_stillImageCapture->cancelCapture();
//    showViewFinder = false;
    m_focusing = false;
    pictureCaptured = true;
    captureImage(); // added this line to restart viewfinder
    emit imageCaptured(preview);
    //qdebug() << "Image sent for decoding";
}

void QCameraControllerWidget::deleteImage(const QString & folderPath)
{
    //qdebug() << "Detected change to folder: " << folderPath;
    QStringList files = imageFolder.entryList(imageFolder.nameFilters());

    for(int i=0; i<files.size(); i++)
    {
        QString image = files[i];
        //qdebug() << "Checking file: " << image;
        if(image.startsWith("camera"))
        {
            QString path(QDesktopServices::storageLocation(QDesktopServices::PicturesLocation));
            QDir pathDir(path);
            pathDir.remove(image);
            //qdebug() << "file deleted: " << image;

    //        QStringList filters;
    //            filters << "*.cpp" << "*.cxx" << "*.cc";
    //            dir.setNameFilters(filters);
        }
//        QMessageBox msgBox;
//        msgBox.setText(image);
//        msgBox.exec();
    }
}

void QCameraControllerWidget::error(QCamera::Error e)
{
    switch (e) {
    case QCamera::NoError:
    {
        break;
    }
    case QCamera::CameraError:
    {
        QMessageBox::warning(this, "QCameraControllerWidget", "General Camera error");
        break;
    }
    case QCamera::InvalidRequestError:
    {
        QMessageBox::warning(this, "QCameraControllerWidget", "Camera invalid request error");
        break;
    }
    case QCamera::ServiceMissingError:
    {
        QMessageBox::warning(this, "QCameraControllerWidget", "Camera service missing error");
        break;
    }
    case QCamera::NotSupportedFeatureError :
    {
        QMessageBox::warning(this, "QCameraControllerWidget", "Camera not supported error");
        break;
    }
    };
}

void QCameraControllerWidget::updateVideo()
{
    if (showViewFinder) {
        repaint();
    }
}

void QCameraControllerWidget::paintEvent(QPaintEvent *event)
{
    //QMainWindow::paintEvent(event);

    QPainter painter(this);
    QRect r = this->rect();

    QFont font = painter.font();
    font.setPixelSize(20);
    painter.setFont(font);
    painter.setPen(Qt::white);

    if (showViewFinder && m_myVideoSurface && m_myVideoSurface->isActive()) {
        // Show view finder
        m_myVideoSurface->paint(&painter);

        // Paint focus message
        if (!m_focusMessage.isEmpty())
            painter.drawText(r, Qt::AlignCenter, m_focusMessage);

    } else {
        painter.fillRect(event->rect(), palette().background());
    }
}

void QCameraControllerWidget::redirectImageSignalFromVideoFinder(QImage image)
{
    emit imageCaptured(image);
}


void QCameraControllerWidget::onZoomIn()
{
    qreal optical = cameraFocus->opticalZoom();
    qreal digital = cameraFocus->digitalZoom();

    if(optical == cameraFocus->maximumOpticalZoom())
        cameraFocus->zoomTo(optical, digital + 0.1);
    else
        cameraFocus->zoomTo(optical + 0.1, 1.0);
}

void QCameraControllerWidget::onZoomOut()
{
    qreal optical = cameraFocus->opticalZoom();
    qreal digital = cameraFocus->digitalZoom();


    if(optical == cameraFocus->maximumOpticalZoom() && digital != 0.0)
    {
        if(digital - 0.1 >= 1)
            cameraFocus->zoomTo(optical, digital - 0.1);
        else
            cameraFocus->zoomTo(optical, 1.0);
    }
    else
    {
        if(optical - 0.1 >= 1)
            cameraFocus->zoomTo(optical - 0.1, 1.0);
        else
            cameraFocus->zoomTo(1.0, 1.0);
    }
}

void QCameraControllerWidget::onStateChanged(QCamera::State state)
{
    if(state == QCamera::ActiveState)
    {
        //      cameraFocus->setFocusMode(QCameraFocus::AutoFocus);
        //      cameraFocus->setFocusPointMode(QCameraFocus::FocusPointAuto);
    }
}

//void QCameraControllerWidget::toggleFlash()
//{
//    if(!m_camera)
//        return;

//    QCameraExposure* cameraexpo = m_camera->exposure();
//    if(!cameraexpo)
//        return;

//    if(!flash->isPressed())
//        cameraexpo->setFlashMode(QCameraExposure::FlashOff);
//    else
//    {
//        if(cameraexpo->isFlashModeSupported(QCameraExposure::FlashTorch))
//        {
//            cameraexpo->setFlashMode(QCameraExposure::FlashTorch);
//            flash->disableBtn(false);
//        }
//        else if(cameraexpo->isFlashModeSupported(QCameraExposure::FlashOn))
//        {
//            cameraexpo->setFlashMode(QCameraExposure::FlashOn);
//            flash->disableBtn(false);
//        }
//        else
//            flash->disableBtn(true);
//    }
//}
