package mindustry.client.ui

import arc.files.Fi
import arc.graphics.*
import arc.input.KeyCode
import arc.scene.event.InputEvent
import arc.scene.event.InputListener
import arc.scene.ui.*
import arc.struct.*
import arc.util.Log
import arc.util.Time
import mindustry.Vars
import mindustry.client.utils.*
import mindustry.ui.dialogs.*

class AttachmentDialog(message: String, attachments: Seq<Texture>) : BaseDialog("@client.attachments") {
    init {
        addCloseButton()
        cont.add(message).center()
        cont.row()
        cont.pane { pane ->
            attachments.forEachIndexed { i, it ->
                pane.row(Image(it))
                pane.addListener(object : InputListener() {
                    override fun touchDown(
                        event: InputEvent?,
                        x: Float,
                        y: Float,
                        pointer: Int,
                        button: KeyCode?
                    ): Boolean {


                        //val pixmap: Pixmap = it.getTextureData().getPixmap()
                        //val pixmap = textureToPixmap(it);
                        ////var file = Fi.get("image.png");
                        //var writer = Platform.FileWriter{}

                        Vars.platform.showFileChooser(false, "png") { file: Fi ->
                            try {
                                //PixmapIO.writePng(file, pixmap)
                                var screen = ScreenDialog(it)
                                screen.show()
                                var screenshot: Pixmap? = null
                                Time.runTask(140f, {screenshot = screen.getScreenshot();PixmapIO.writePng(file, screenshot)})
                                Time.runTask(160f, screen::hide)

                                /*
                                Timer.schedule(object : TimerTask() {
                                    override fun run() {

                                        PixmapIO.writePng(file, screenshot)
                                        //Time.runTask(160f, screen::hide)
                                    }
                                }, 1f, 1f)
                                 */


                                //PixmapIO.writePng(file, pixmap)
                                //pixmap.dispose()
                            } catch (e: Throwable) {
                                Vars.ui.showException(e)
                                Log.err(e)
                            }
                        }
                        //pixmap.dispose()

                        return true
                    }
                })
            }
        }.fill()
        show()
    }
}