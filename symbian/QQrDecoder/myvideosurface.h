#ifndef MYVIDEOSURFACE_H
#define MYVIDEOSURFACE_H

#include <QWidget>
#include <QVideoFrame>
#include <QImage>
#include <QVideoSurfaceFormat>
#include <QAbstractVideoSurface>
#include <QVideoRendererControl>
#include <QVideoSurfaceFormat>
#include <QPainter>
#include <QTimer>

class VideoIF
{
public:
    virtual void updateVideo() = 0;
};

class MyVideoSurface: public QAbstractVideoSurface
{
Q_OBJECT

public:
    MyVideoSurface(QWidget* widget, VideoIF* target, QObject * parent = 0);
    ~MyVideoSurface();

    bool start(const QVideoSurfaceFormat &format);

    bool present(const QVideoFrame &frame);

    QList<QVideoFrame::PixelFormat> supportedPixelFormats(
                QAbstractVideoBuffer::HandleType handleType = QAbstractVideoBuffer::NoHandle) const;

    void paint(QPainter*);

public slots:
    void sendImageToDecode();

signals:
    void imageCaptured(QImage image);

private:
    QWidget* m_targetWidget;
    VideoIF* m_target;
    QVideoFrame m_frame;
    QImage::Format m_imageFormat;
    QVideoSurfaceFormat m_videoFormat;
    QTimer* timer;
};

#endif // MYVIDEOSURFACE_H
