package mindustry.client.ui

import arc.Core
import arc.graphics.*
import arc.graphics.g2d.TextureRegion
import arc.scene.style.TextureRegionDrawable
import arc.scene.ui.*
import arc.scene.ui.layout.*
import arc.util.*
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
            if (Core.app.getClipboardText().isNotEmpty()) {
                if (Core.app.getClipboardText().startsWith("mrks add")) {
                    var clipboardText = Core.app.getClipboardText().split(' ')
                    var x = 0
                    var y = 0
                    var color = ""
                    var name = ""
                    clipboardText.forEachIndexed { i, element ->
                        if (i == 2) x = element.toInt()
                        if (i == 3) y = element.toInt()
                        if (i == 4) color = element
                        if (i > 4) name += element
                    }
                    Markers.add(
                        Markers.Marker(x, y, name, Color.valueOf(color))
                    )
                    updatePane()
                }


                val text = Core.app.getClipboardText().trim()

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
                            val name = parts.subList(4, parts.size).joinToString(" ")

                            try {
                                val color = Color.valueOf(colorHex)

                                Markers.add(Markers.Marker(x, y, name, color))
                            } catch (e: Throwable) {
                                Log.err("Invalid color: $colorHex")
                            }
                        }
                    }

                    updatePane()
                }

            }
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
                Core.app.setClipboardText("mrks add ${marker.x} ${marker.y} ${marker.color} ${marker.name}")
                ui.showInfoFade("@copied");
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
            builder.appendLine("  add ${marker.x} ${marker.y} ${marker.color} ${marker.name}")
        }
        builder.append("}")

        Core.app.setClipboardText(builder.toString())
        ui.showInfoFade("@copied")
    }
}
