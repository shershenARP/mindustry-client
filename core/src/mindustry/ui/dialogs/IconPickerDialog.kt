package mindustry.ui.dialogs

import arc.scene.event.HandCursorListener
import arc.scene.event.Touchable
import arc.scene.style.TextureRegionDrawable
import arc.scene.ui.Dialog
import arc.scene.ui.layout.Cell
import mindustry.gen.Icon
import java.lang.reflect.Field
import java.util.*

class IconPickerDialog(
    title: String = "Select Icon",
    private val onSelect: (TextureRegionDrawable) -> Unit
) : Dialog(title) {

    init {
        val fields: Array<Field> = Icon::class.java.declaredFields
        Arrays.sort(fields) { a, b -> a.name.compareTo(b.name) }

        val columns = 10  // количество столбцов
        var count = 0

        cont.apply {
            table { t ->
                for (f in fields) {
                    if (f.name.endsWith("Small")) continue

                    val region = try {
                        @Suppress("UNCHECKED_CAST")
                        f.get(null) as? TextureRegionDrawable
                    } catch (e: Throwable) {
                        null
                    } ?: continue

                    t.table { cellTable ->
                        val imgCell = cellTable.image(region).size(32f).touchable(Touchable.enabled)
                        val imgActor = imgCell.get()
                        imgActor.touchable = Touchable.enabled
                        addListener(HandCursorListener())
                        imgActor.clicked {
                            onSelect(region)
                            hide()
                        }
                    }.pad(5f).growX().center()

                    count++
                    if (count % columns == 0) {
                        t.row()
                    }
                }
            }
        }

        buttons.defaults().size(120f, 50f)
        buttons.button("Close") { hide() }
        buttons.row()
    }
}
