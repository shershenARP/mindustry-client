package mindustry.client.communication

import arc.Core
import arc.graphics.Color
import arc.input.KeyCode
import arc.scene.style.TextureRegionDrawable
import arc.scene.ui.CheckBox
import arc.scene.ui.Image
import arc.scene.ui.Label
import arc.scene.ui.ScrollPane
import arc.scene.ui.layout.Table
import mindustry.Vars.player
import mindustry.Vars.ui
import mindustry.client.navigation.Markers
import mindustry.client.ui.MarkerDialog.addCloseListener
import mindustry.client.ui.MarkerDialog.keyDown
import mindustry.client.ui.MarkerDialog.nameFromDrawable
import mindustry.gen.Groups
import mindustry.gen.Icon
import mindustry.gen.Tex
import mindustry.graphics.Pal
import mindustry.ui.Styles
import mindustry.ui.dialogs.BaseDialog
import mindustry.ui.dialogs.IconPickerDialog
import mindustry.ui.fragments.ChatFragment.ChatMessage
import kotlin.random.Random

class MarkerTransmission : Transmission {
    override var id = Random.nextLong()
    override val secureOnly = false
    var encoded: String
    var senderID: Int = -1

    //отправка
    constructor(data: Markers.Marker) {
        /* var words = data.split(" ")
        var data = "mrks"
        words.forEach() {
            data += " ${it}"
        }
         */
        encoded = Base32768Coder.encode("mrks add ${data.x} ${data.y} ${data.color} ${nameFromDrawable(data.shape)} ${data.name}")
    }
    constructor(data: List<Markers.Marker>) {
        /* var words = data.split(" ")
        var data = "mrks"
        words.forEach() {
            data += " ${it}"
        }
         */
        val builder = StringBuilder()
        builder.appendLine("mrks list")
        for (marker in data) {
            builder.appendLine(" add ${marker.x} ${marker.y} ${marker.color} ${nameFromDrawable(marker.shape)} ${marker.name}")
        }
        builder.append("}")
        encoded = Base32768Coder.encode(builder.toString())
    }

    //восстановление
    @Suppress("UNUSED_PARAMETER")
    constructor(bytes: ByteArray, id: Long, senderID: Int) {
        this.id = id
        this.encoded = String(bytes, Charsets.UTF_8)
        this.senderID = senderID
    }

    override fun serialize(): ByteArray {
        return encoded.toByteArray(Charsets.UTF_8)
    }

    fun getDecoded(): String {
        return Base32768Coder.decodeString(encoded)
    }

    fun addToChat() {
        val message: ChatMessage = ui.chatfrag.addMsg(
            Core.bundle.format("client.marker.chatsharemessage", Groups.player.getByID(this.senderID).name)
        )

        message.addButton(0, message.message.length) {
//            Vars.ui.showInfo(markerText.toString())
            var markerText = this.getDecoded()
            if (markerText.startsWith("mrks add")) {
                importMarker()
            } else {
                importMarkers()
            }
        }
    }

    fun importMarker() {
        var markerText = this.getDecoded().split(" ")
        var marker = Markers.Marker(markerText[2].toInt(), markerText[3].toInt(), markerText.subList(6, markerText.size).joinToString(" "), Color.valueOf(markerText[4]), Icon.icons.get(markerText[5]))

        val inMarkers = senderID == player.id || Markers.contains(marker)
//            var inMarkers = false;
        if (Core.settings.getBool("icompletelytrustotherclients")) {
            if (!inMarkers) Markers.add(marker)
        }
        var dialog = BaseDialog("@client.marker.importDialog")
        dialog.cont.add(Image(marker.shape)).left().pad(5f).get().clicked {
            val dialog = IconPickerDialog("Select Icon") { icon ->
                marker.shape = TextureRegionDrawable(icon)
            }
            dialog.show()
        }

        dialog.cont.stack(Image(Tex.alphaBg), Image(Tex.whiteui).apply {
            update { setColor(marker.color) }
        }).size(50f).pad(5f).left().get().clicked {
            ui.picker.show(marker.color) { color ->
                marker.color = Color(color)
            }
        }

        dialog.cont.add(marker.name).left().pad(5f).get().clicked {
            ui.showTextInput("Name", "Name", marker.name) {
                if (it.isNotBlank()) {
                    marker.name = it
                }
            }
        }

        dialog.cont.add("(${marker.x}, ${marker.y})").get().clicked {
            ui.showTextInput("Coordinates", "Coordinates", "${marker.x}, ${marker.y}") {
                if (it.matches("\\(?\\d+, ?\\d+\\)?".toRegex())) {
                    val matches = "\\d+".toRegex().findAll(it)
                    marker.x = matches.first().value.toInt()
                    marker.y = matches.last().value.toInt()
                }
            }
        }

        dialog.cont.row()


        val buttonTable = Table()

        if (inMarkers) {
            buttonTable.button("@ok", Icon.ok) {}.disabled(true).growX()
        } else {
            buttonTable.button("@save", Icon.save) {
                Markers.add(marker)
                dialog.hide()
            }.growX()
        }

        buttonTable.row()

        dialog.closeOnBack()
        buttonTable.button("@back", Icon.left, dialog::hide).size(210f, 64f);
        dialog.buttons.add(buttonTable).growX()
        dialog.show()
    }
    class BoolRef(var value: Boolean)

    fun importMarkers() {
        val text = this.getDecoded()
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.startsWith("add") }

        val parsedMarkers = mutableListOf<Pair<Markers.Marker, BoolRef>>()

        for (line in lines) {
            val parts = line.split(" ")
            if (parts.size >= 5) {
                val x = parts[1].toIntOrNull() ?: continue
                val y = parts[2].toIntOrNull() ?: continue
                val colorHex = parts[3]
                val shape = Icon.icons.get(parts[4])
                val name = parts.subList(5, parts.size).joinToString(" ")
                val marker = Markers.Marker(x, y, name, Color.valueOf(colorHex), shape)

                parsedMarkers += marker to BoolRef(true)
            }
        }

        if (parsedMarkers.isEmpty()) {
            ui.showInfoToast("@client.marker.noValidMarkersFound", 20f)
            return
        }

        val dialog = BaseDialog("@client.marker.importsDialog")

        val scroll = Table()
        val pane = ScrollPane(scroll, Styles.smallPane)
        pane.setScrollingDisabled(true, false)

        for ((marker, enabled) in parsedMarkers) {
            val row = Table()

            val nameLabel = Label(marker.name)
            val coordLabel = Label("(${marker.x}, ${marker.y})")

            val inMarkers = senderID == player.id || Markers.contains(marker)

            val checkbox = CheckBox("").apply {
                isChecked = !inMarkers
                isDisabled = inMarkers
                changed {
                    enabled.value = isChecked
                }
            }
            row.add(checkbox).padRight(5f)

            val iconImage = Image(marker.shape)
            row.add(iconImage).left().pad(5f).get().clicked {
                val dialog = IconPickerDialog("Select Icon") { icon ->
                    marker.shape = TextureRegionDrawable(icon)
                    iconImage.setDrawable(marker.shape) // обновляем вручную
                }
                dialog.show()
            }

            val colorBg = Image(Tex.whiteui)
            row.stack(Image(Tex.alphaBg), colorBg).size(50f).pad(5f).left().get().clicked {
                ui.picker.show(marker.color) { color ->
                    marker.color = Color(color)
                }
            }

            row.add(nameLabel).left().pad(5f).get().clicked {
                ui.showTextInput("Name", "Name", marker.name) {
                    if (it.isNotBlank()) {
                        marker.name = it
                        nameLabel.setText(it)
                    }
                }
            }

            row.add(coordLabel).get().clicked {
                ui.showTextInput("Coordinates", "Coordinates", "${marker.x}, ${marker.y}") {
                    if (it.matches("\\(?\\d+, ?\\d+\\)?".toRegex())) {
                        val matches = "\\d+".toRegex().findAll(it)
                        marker.x = matches.first().value.toInt()
                        marker.y = matches.last().value.toInt()
                        coordLabel.setText("(${marker.x}, ${marker.y})")
                    }
                }
            }

            val statusLabel = Label("")
            row.add(statusLabel)

            row.update {
                colorBg.setColor(marker.color)
                nameLabel.setText(marker.name)
                coordLabel.setText("(${marker.x}, ${marker.y})")

                val exists = senderID == player.id || Markers.contains(marker)
                checkbox.isDisabled = exists
                checkbox.isChecked = !exists && enabled.value
                statusLabel.setText(
                    if (exists) "[red]\uE815"//"@client.marker.alreadyExists"
                    else "[stat]\uE800"//"@client.marker.willBeAdded"
                )
            }

            scroll.add(row).growX().pad(4f)
            scroll.row()
        }

        dialog.cont.add(pane).maxHeight(300f).padBottom(10f)
        dialog.cont.row()

        val buttonTable = Table()

        buttonTable.button("@client.marker.importAll", Icon.download) {
            var added = 0
            for ((marker, enabled) in parsedMarkers) {
                val inMarkers = senderID == player.id || Markers.contains(marker)
//                val inMarkers = false
                if (!inMarkers && enabled.value) {
                    Markers.add(marker)
                    added++
                }
            }
            ui.showInfoToast(
                Core.bundle.format("@client.marker.importedCount", added),
                3f
            )
            dialog.hide()
        }.growX().padBottom(5f)

        buttonTable.row()

        buttonTable.button("@back", Icon.left, dialog::hide).size(210f, 64f)

        dialog.buttons.add(buttonTable).growX()
        dialog.closeOnBack()
        dialog.show()
    }


}
