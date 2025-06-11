package mindustry.client.ui

import arc.files.Fi
import arc.graphics.*
import arc.input.KeyCode
import arc.math.Mathf
import arc.scene.event.InputEvent
import arc.scene.event.InputListener
import arc.scene.ui.*
import arc.struct.*
import arc.util.Log
import arc.util.ScreenUtils
import arc.util.Time
import mindustry.Vars
import mindustry.client.utils.*
import mindustry.core.Platform
import mindustry.ui.dialogs.*
import mindustry.ui.fragments.ChatFragment

class ScreenDialog(attachment: Texture) : BaseDialog("@client.attachments") {
    private var screenshot: Pixmap? = null

    init {
        var image: Image = Image(attachment)
        cont.add(image)
        show()
        //знаю, задержка могла бы быть меньше, но это на всякий случай. Позже исправлю. Наверное...
        //Time.runTask(120f, {screenshot = ScreenUtils.getFrameBufferPixmap(10, 100, attachment.width - 18, attachment.height - 20)})
        Time.runTask(120f, {screenshot = ScreenUtils.getFrameBufferPixmap(Mathf.floor(image.x) + 7, Mathf.floor(image.y) + 11, Mathf.floor(image.imageWidth) - 2, Mathf.floor(image.imageHeight), true)})
        Log.info("ix @, iy @", image.imageX, image.imageY)
        Log.info("x @, y @", Mathf.floor(image.x), Mathf.floor(image.y))
        Log.info("iw @, ih @", image.imageWidth, image.imageHeight)
        Log.info("w @, h @", Mathf.floor(image.width), Mathf.floor(image.height))
    }

    fun getScreenshot(): Pixmap? {
        return screenshot
    }
}