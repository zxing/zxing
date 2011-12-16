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

#include "button.h"
#include <QMouseEvent>
#include <QPainter>
#include <QTimer>

Button::Button(QWidget *parent, Qt::WindowFlags f) :
    QLabel(parent, f)
{
    m_downPixmap = 0;
    m_disabled = false;
    m_toggled = false;
    m_toggle_enabled = false;
}

Button::~Button()
{
}

void Button::disableBtn(bool b)
{
    m_disabled = b;
    setPressed(m_disabled);
}

void Button::mousePressEvent(QMouseEvent *event)
{
    if (!m_disabled) {
        event->accept();
        setPixmap(m_downPixmap);
        repaint();
        // Lift button back to up after 300ms

        if(m_toggle_enabled)
        {
            m_toggled = !m_toggled;
            setPressed(m_toggled);
            emit pressed();
        }
        else
            QTimer::singleShot(300, this, SLOT(backToUp()));
    }
}

void Button::backToUp()
{
    setPixmap(m_upPixmap);
    repaint();
    emit pressed();
}

void Button::setPixmap(const QPixmap& p)
{
    // Set up and down picture for the button
    // Set pixmap
    if (!p.isNull())
        QLabel::setPixmap(p);

    // Make down pixmap if it does not exists
    if (m_downPixmap.isNull()) {
        // Store up pixmap
        m_upPixmap = *pixmap();

        // Create down pixmap
        // Make m_downPixmap as a transparent m_upPixmap
        QPixmap transparent(m_upPixmap.size());
        transparent.fill(Qt::transparent);
        QPainter painter(&transparent);
        painter.setCompositionMode(QPainter::CompositionMode_Source);
        painter.drawPixmap(0, 0, m_upPixmap);
        painter.setCompositionMode(QPainter::CompositionMode_DestinationIn);
        painter.fillRect(transparent.rect(), QColor(0, 0, 0, 150));
        painter.end();
        m_downPixmap = transparent;
    }
}

void Button::enableToggle(bool enable)
{
    m_toggle_enabled = enable;
}

void Button::setPressed(bool isPressed)
{
    if(isPressed)
    {
        m_toggled = true;
        setPixmap(m_downPixmap);
    }
    else
    {
        m_toggled = false;
        setPixmap(m_upPixmap);
    }
}

bool Button::isPressed()
{
    return m_toggled;
}
