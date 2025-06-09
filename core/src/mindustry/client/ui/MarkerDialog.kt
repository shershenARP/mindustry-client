package mindustry.client.ui

import arc.Core
import arc.graphics.*
import arc.graphics.g2d.Font
import arc.graphics.g2d.TextureRegion
import arc.scene.style.TextureRegionDrawable
import arc.scene.ui.*
import arc.scene.ui.layout.*
import arc.util.*
import arc.util.serialization.Base64Coder
import mindustry.FFF.B64
import mindustry.Vars.*
import mindustry.client.navigation.*
import mindustry.client.utils.*
import mindustry.gen.*
import mindustry.ui.dialogs.*
import kotlin.random.*

object MarkerDialog : BaseDialog("Markers") {
    val pane = Table()
    init {
        cont.add("Minimap Markers").center().top()
        cont.row()
        cont.pane(pane).width(800f).grow()
        pane.add().width(pane.width)
        cont.row()
        buttons.button(Icon.upload) {
            importMarkers();
        }
        buttons.button(Icon.export) {
            exportMarkers(Markers)
        }.right().pad(5f)
        buttons.button(Icon.add) { MarkerAddDialog().show() }.growX()
        buttons.button("Close") { hide() }.growX()
        addCloseListener()
        updatePane()

        shown { updatePane() }
    }

    private fun updatePane() {
        pane.clearChildren()

        for (marker in Markers) {
            val table = Table()
            table.margin(15f)
            table.width = pane.width
            //table.image(marker.shape).left().pad(5f)//.width(20f)
            table.add(Image(marker.shape)).left().pad(5f).get().clicked {
                val dialog = IconPickerDialog("Select Icon") { icon ->
                    marker.shape = TextureRegionDrawable(icon)
                    updatePane()
                }
                dialog.show()
            }

            /*
            table.stack(Image(Tex.alphaBg), Image(Tex.whiteui).apply {
                update { setColor(marker.color) }
            }).size(50f).pad(5f).left().get().clicked {
                ui.picker.show(marker.color) { color -> marker.color = color }
            }
            */

            table.stack(Image(Tex.alphaBg), Image(Tex.whiteui).apply {
                update { setColor(marker.color) }
            }).size(50f).pad(5f).left().get().clicked {
                ui.picker.show(marker.color) { color ->
                    marker.color = Color(color)
                }
            }

            table.add(marker.name).left().pad(5f).growX().get().clicked {
                ui.showTextInput("Name", "Name", marker.name) {
                    if (it.isNotBlank()) {
                        marker.name = it
                        updatePane()
                    }
                }
            }

            table.add().growX()

            table.add("(${marker.x}, ${marker.y})").right().pad(5f).get().clicked {
                ui.showTextInput("Coordinates", "Coordinates", "${marker.x}, ${marker.y}") {
                    if (it.matches("\\(?\\d+, ?\\d+\\)?".toRegex())) {
                        val matches = "\\d+".toRegex().findAll(it)
                        marker.x = matches.first().value.toInt()
                        marker.y = matches.last().value.toInt()
                        updatePane()
                    }
                }
            }

            table.button(Icon.trash) {
                Markers.remove(marker)
                updatePane()
            }.right().pad(5f)//.width(50f)

            table.button(Icon.export) {
                var text = B64.write("mrks add ${marker.x} ${marker.y} ${marker.color} ${nameFromDrawable(marker.shape)} ${marker.name}")
                Core.app.setClipboardText(text)
                ui.showInfoFade("@copied");
                updatePane()
            }.right().pad(5f)

            pane.row(table).growX()
        }
    }

    class MarkerAddDialog : BaseDialog("Add Marker") {
        init {
            val nameField = TextField("")
            nameField.messageText = "Name"
            cont.row(nameField)

            val xField = cont.field("") {}.valid(Strings::canParsePositiveInt).growX().get()
            xField.messageText = "X"
            val yField = cont.field("") {}.valid(Strings::canParsePositiveInt).growX().get()
            yField.messageText = "Y"

            cont.row()

            var color = Color.HSVtoRGB(Random.nextFloat() * 360, 75f, 75f)

            val image = Image(Icon.star).apply {
                setColor(color)
            }

            cont.stack(Image(Tex.alphaBg), Image(Tex.whiteui).apply {
                update { setColor(color) }
            }).size(50f).pad(5f).left().get().clicked {
                ui.picker.show(color) { col ->
                    color = col
                    image?.setColor(color)
                }
            }
            cont.row()
            cont.add(image).size(60f).left().get().clicked {
                val dialog = IconPickerDialog("Select Icon") { icon ->
                    image.setDrawable(icon)
                    image.setColor(color)
                    //update {
                    //image(icon) угарно вышло
                    //}
                }
                dialog.show()
            }

            buttons.button("Ok") {
                if (xField.isValid && yField.isValid && nameField.text.isNotBlank()) {
                    Markers.add(
                        Markers.Marker(xField.text.toInt(), yField.text.toInt(), nameField.text, color, TextureRegionDrawable(image.region))
                    )
                    updatePane()
                    hide()
                }
            }.growX()

            buttons.button("@back", Icon.left, this::hide).growX()
        }
    }

    fun exportMarkers(markers: Markers) {
        val builder = StringBuilder()
        builder.appendLine("mrks list")
        for (marker in markers) {
            builder.appendLine(" add ${marker.x} ${marker.y} ${marker.color} ${nameFromDrawable(marker.shape)} ${marker.name}")
        }
        builder.append("}")

        Core.app.setClipboardText(B64.write(builder.toString()))
        ui.showInfoFade("@copied")
    }

    fun importMarkers() {
        if (Core.app.getClipboardText().isNotEmpty()) {
            try {
                var text = B64.read(Core.app.getClipboardText());
                if (text.startsWith("mrks add")) {
                    var clipboardText = text.split(' ')
                    if (clipboardText.size >= 6) {
                        var x = clipboardText[2].toInt()
                        var y = clipboardText[3].toInt()
                        var color = clipboardText[4]
                        var shape = Icon.icons.get(clipboardText[5])
                        var name = clipboardText.subList(6, clipboardText.size).joinToString(" ")

                        try {
                            Markers.add(Markers.Marker(x, y, name, Color.valueOf(color), shape))
                        } catch (e: Throwable) {
                            ui.showErrorMessage("Invalid color/shap: ${color} ${shape}")
                        }

                        updatePane()
                    }
                }

                if (text.startsWith("mrks list") && text.endsWith("}")) {
                    val lines = text.lines()
                        .map { it.trim() }
                        .filter { it.startsWith("add") }

                    for (line in lines) {
                        val parts = line.split(" ")

                        if (parts.size >= 5) {
                            val x = parts[1].toIntOrNull() ?: continue
                            val y = parts[2].toIntOrNull() ?: continue
                            val colorHex = parts[3]
                            var shape = Icon.icons.get(parts[4])
                            val name = parts.subList(5, parts.size).joinToString(" ")

                            try {
                                Markers.add(Markers.Marker(x, y, name, Color.valueOf(colorHex), shape))
                            } catch (e: Throwable) {
                                ui.showErrorMessage("Invalid color/shap: ${colorHex} ${shape}")
                            }
                        }
                    }
                    updatePane()
                }
            } catch (e: Throwable) {
                ui.showErrorMessage("Invalid")
            }
        }
    }
    //дальше кромешный ад
    //*рабочий кромешный ад
    fun regionsEqual(a: TextureRegion, b: TextureRegion): Boolean {
        return a.texture == b.texture &&
                a.x == b.x &&
                a.y == b.y &&
                a.width == b.width &&
                a.height == b.height
    }

    fun nameFromDrawable(drawable: TextureRegionDrawable): String? {
        val region = drawable.region
        return Icon.icons.entries().find { regionsEqual(it.value.region, region) }?.key
    }
    //сам в шоке, котлин - ужасен, а фус на нём полностью написан. Это ли не печально?
}
