/****************************************************************************
 **
 ** Trolltech hereby grants a license to use the Qt/Eclipse Integration
 ** plug-in (the software contained herein), in binary form, solely for the
 ** purpose of creating code to be used with Trolltech's Qt software.
 **
 ** Qt Designer is licensed under the terms of the GNU General Public
 ** License versions 2.0 and 3.0 ("GPL License"). Trolltech offers users the
 ** right to use certain no GPL licensed software under the terms of its GPL
 ** Exception version 1.2 (http://trolltech.com/products/qt/gplexception).
 **
 ** THIS SOFTWARE IS PROVIDED BY TROLLTECH AND ITS CONTRIBUTORS (IF ANY) "AS
 ** IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 ** TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 ** PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 ** OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 ** EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 ** PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 ** PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 ** LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 ** NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 ** SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
 **
 ** Since we now have the GPL exception I think that the "special exception
 ** is no longer needed. The license text proposed above (other than the
 ** special exception portion of it) is the BSD license and we have added
 ** the BSD license as a permissible license under the exception.
 **
 ****************************************************************************/

#include "QQrDecoder.h"
//#include <zxing/qrcode/QRCodeReader.h>
#include <zxing/MultiFormatReader.h>

#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/Binarizer.h>
#include <zxing/BinaryBitmap.h>
#include <QFileDialog>
#include <QGraphicsView>
#include <QPainter>
#include <QPoint>
#include <QPixmap>

using namespace zxing;
//using namespace zxing::qrcode;

QQrDecoder::QQrDecoder(QWidget *parent): QMainWindow(parent)
{
    ui.setupUi(this);
    connect(ui.startDecode, SIGNAL(clicked()), this, SLOT(findAndDecode()));
    connect(ui.cameraWidget, SIGNAL(imageCaptured(QImage)), this, SLOT(decodeImage(QImage)));
    connect(ui.cameraWidget, SIGNAL(logMessage(QString)), ui.resultLabel, SLOT(setText(QString)));
}

QQrDecoder::~QQrDecoder()
{
}

void QQrDecoder::InitializeSymbianCamera()
{

}

void QQrDecoder::findAndDecode()
{
    ui.cameraWidget->CaptureImage();
}

void QQrDecoder::decodeImage(QImage originalImage)
{
    MultiFormatReader decoder;

    image.setImage(originalImage);

    Ref<Result> res;

    try{
        Ref<LuminanceSource> imageRef(new CameraImageWrapper(image));
        GlobalHistogramBinarizer* binz = new GlobalHistogramBinarizer(imageRef);

        Ref<Binarizer> bz (binz);
        BinaryBitmap* bb = new BinaryBitmap(bz);

        Ref<BinaryBitmap> ref(bb);

        res = decoder.decode(ref);

        QString string = QString(res->getText()->getText().c_str());
        ui.resultLabel->setText(string);
    }
    catch(zxing::Exception& e)
    {
        ui.resultLabel->setText("Error");
    }
}

void QQrDecoder::reloadFormatedPicture(int x)
{
}

