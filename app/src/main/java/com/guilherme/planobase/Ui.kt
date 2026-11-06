package com.guilherme.planobase

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

object C {
    val bg = Color(0xFF0E1412)
    val surface = Color(0xFF17201D)
    val line = Color(0xFF2A3632)
    val fg = Color(0xFFE7EEEB)
    val muted = Color(0xFF9AAAA4)
    val accent = Color(0xFF4DBF9C)
    val ink = Color(0xFF06221A)
    val signal = Color(0xFFF0B04E)
    val rest = Color(0xFF7FAEE0)
    val prop = Color(0xFF3A4A44)
}
val demoColors = DemoColors(body = C.fg, far = C.muted, head = C.accent, prop = C.prop, floor = C.line)
val Display = FontFamily(Typeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)))
val Tnum = TextStyle(fontFeatureSettings = "tnum")
private val DOW = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

fun fmtDay(iso: String): String {
    val d = LocalDate.parse(iso)
    return "${DOW[d.dayOfWeek.value - 1]}, ${d.dayOfMonth}/${"%02d".format(d.monthValue)}"
}
fun kg(v: Double) = "%.1f".format(v).replace('.', ',')

/* ---------- peças de interface ---------- */

@Composable fun Eyebrow(t: String, color: Color = C.muted) =
    Text(t.uppercase(), color = color, fontSize = 12.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.SemiBold, fontFamily = Display)

@Composable fun Title(t: String, size: TextUnit = 22.sp, color: Color = C.fg, align: TextAlign? = null) =
    Text(t, color = color, fontSize = size, fontFamily = Display, fontWeight = FontWeight.Bold, lineHeight = size * 1.08, textAlign = align)

@Composable fun Body(t: String, color: Color = C.fg, size: TextUnit = 15.sp, align: TextAlign? = null) =
    Text(t, color = color, fontSize = size, lineHeight = size * 1.45, textAlign = align)

@Composable fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.surface)
            .border(1.dp, C.line, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)

@Composable fun Btn(t: String, modifier: Modifier = Modifier, ghost: Boolean = false, size: TextUnit = 19.sp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.clip(shape).then(if (ghost) Modifier.border(1.5.dp, C.line, shape) else Modifier.background(C.accent))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(t.uppercase(), color = if (ghost) C.fg else C.ink, fontFamily = Display, fontWeight = FontWeight.Bold,
            fontSize = size, letterSpacing = 1.sp, textAlign = TextAlign.Center)
    }
}

@Composable fun Stat(value: String, label: String) = Column {
    Text(value, color = C.fg, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, style = Tnum)
    Text(label, color = C.muted, fontSize = 12.sp)
}

@Composable fun Choice(selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.clip(shape).background(if (selected) C.accent.copy(alpha = 0.12f) else C.surface)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) C.accent else C.line, shape)
            .clickable(onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
}

/* ---------- raiz ---------- */

@Composable
fun AppRoot() {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = C.accent, onPrimary = C.ink, background = C.bg, onBackground = C.fg,
        surface = C.surface, onSurface = C.fg, outline = C.line, onSurfaceVariant = C.muted)) {
        val ps by Engine.state.collectAsStateWithLifecycle()
        Box(Modifier.fillMaxSize().background(C.bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (ps.active) PlayerScreen(ps) else MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Peso corporal · sem impacto")
                    Title("TREINO", 36.sp)
                }
                Eyebrow("Nível ${Store.level}", C.accent)
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.surface)
                    .border(1.dp, C.line, RoundedCornerShape(12.dp)).padding(4.dp),
            ) {
                listOf("Hoje", "Treinos", "Progresso").forEachIndexed { i, t ->
                    val sel = tab == i
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (sel) C.accent else Color.Transparent)
                            .clickable { tab = i }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(t.uppercase(), color = if (sel) C.ink else C.muted, fontFamily = Display,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.8.sp)
                    }
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> HomeTab()
                1 -> WorkoutsTab()
                else -> ProgressTab()
            }
        }
    }
}

private val listPad = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)

/* ---------- Hoje ---------- */

@Composable
fun HomeTab() {
    val ctx = LocalContext.current
    val today = LocalDate.now()
    val log = Store.log.toList()
    val key = Plan.nextKey(log)
    val lvl = Store.level
    val l = Plan.levels.getValue(lvl)
    val w = Plan.workout(key)
    val doneToday = log.any { it.date == today.toString() && !it.partial }

    LazyColumn(contentPadding = listPad, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            val ws = Plan.weekStart(today)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0..6) {
                    val d = ws.plusDays(i.toLong())
                    val done = log.lastOrNull { it.date == d.toString() }
                    val planned = Plan.planDay[d.dayOfWeek.value]
                    val isToday = d == today
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(C.surface)
                            .border(if (isToday) 2.dp else 1.dp, if (isToday) C.accent else C.line, RoundedCornerShape(10.dp))
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(DOW[i], color = C.muted, fontSize = 11.sp)
                        Text("${d.dayOfMonth}", color = C.fg, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        val mod = Modifier.size(22.dp).clip(CircleShape)
                        when {
                            done != null -> Box(mod.background(C.accent), contentAlignment = Alignment.Center) {
                                Text(done.workout, color = C.ink, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            planned != null -> Box(mod.border(1.5.dp, C.muted, CircleShape), contentAlignment = Alignment.Center) {
                                Text(planned, color = C.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            else -> Box(mod.border(1.dp, C.line, CircleShape))
                        }
                    }
                }
            }
        }
        item {
            Card {
                Eyebrow(if (doneToday) "Feito hoje · próximo da fila" else "Próximo treino")
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Title(key, 72.sp, C.accent)
                    Column(Modifier.padding(bottom = 6.dp)) {
                        Title(w.name, 26.sp)
                        Body(w.focus, C.muted, 14.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Stat("${Plan.totalMinutes(key, lvl)}", "minutos")
                    Stat("${l.rounds}", "rodadas")
                    Stat("${l.work}/${l.rest}", "seg. esforço/descanso")
                }
                Btn("Começar treino", Modifier.fillMaxWidth()) { Engine.start(ctx, key, lvl) }
            }
        }
        item {
            Card {
                Title("Seu nível", 20.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (n in 1..3) {
                        val lv = Plan.levels.getValue(n)
                        Choice(n == lvl, Modifier.weight(1f), onClick = { Store.setLevelTo(n) }) {
                            Text("Nível $n", color = C.fg, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("${lv.label}\n${lv.work}s / ${lv.rest}s · ${lv.rounds} rodadas", color = C.muted, fontSize = 11.sp, lineHeight = 14.sp)
                        }
                    }
                }
                Body(Plan.levelHint(lvl, log), C.muted, 14.sp)
            }
        }
        item { ReminderCard() }
        item {
            Card {
                Title("Semana padrão", 20.sp)
                Column {
                    listOf("Segunda" to "Treino A", "Quarta" to "Treino B", "Sexta" to "Treino C",
                        "Ter, Qui, Sáb" to "Caminhada leve 20–30 min", "Domingo" to "Descanso").forEach { (a, b) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                            Body(a, size = 14.sp); Spacer(Modifier.weight(1f)); Body(b, C.muted, 14.sp)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
                    }
                }
                Body("Perdeu um dia? Faça o próximo treino da fila. O app segue a ordem A → B → C.", C.muted, 13.sp)
            }
        }
    }
}

@Composable
fun ReminderCard() {
    val ctx = LocalContext.current
    val r = Store.reminder
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Title("Lembrete", 20.sp)
                Body("Notificação nos dias marcados, se o treino ainda não foi feito.", C.muted, 13.sp)
            }
            Switch(r.enabled, onCheckedChange = { Store.setReminderTo(ctx, r.copy(enabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = C.ink, checkedTrackColor = C.accent,
                    uncheckedThumbColor = C.muted, uncheckedTrackColor = C.bg, uncheckedBorderColor = C.line))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Body("Horário", size = 15.sp)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).border(1.5.dp, C.line, RoundedCornerShape(10.dp))
                    .clickable {
                        TimePickerDialog(ctx, { _, h, m -> Store.setReminderTo(ctx, Store.reminder.copy(hour = h, minute = m, enabled = true)) },
                            r.hour, r.minute, true).show()
                    }.padding(horizontal = 14.dp, vertical = 6.dp),
            ) { Text("%02d:%02d".format(r.hour, r.minute), color = C.fg, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, style = Tnum) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEachIndexed { i, t ->
                val day = i + 1
                val on = day in r.days
                Box(
                    Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                        .background(if (on) C.accent else Color.Transparent)
                        .border(1.5.dp, if (on) C.accent else C.line, CircleShape)
                        .clickable { Store.setReminderTo(ctx, r.copy(days = if (on) r.days - day else r.days + day)) },
                    contentAlignment = Alignment.Center,
                ) { Text(t, color = if (on) C.ink else C.muted, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
        }
    }
}

/* ---------- Treinos ---------- */

@Composable
fun WorkoutsTab() {
    val ctx = LocalContext.current
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val lvl = Store.level
    LazyColumn(contentPadding = listPad, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Body("Toque num exercício para ver a demonstração, o passo a passo e vídeos.", C.muted, 14.sp) }
        Plan.workouts.forEach { w ->
            item(key = w.key) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.surface)
                        .border(1.dp, C.line, RoundedCornerShape(16.dp)),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Title(w.key, 44.sp, C.accent)
                        Column {
                            Title(w.name, 20.sp)
                            Body("${w.focus} · ${Plan.totalMinutes(w.key, lvl)} min no nível $lvl", C.muted, 13.sp)
                        }
                    }
                    w.exercises.forEach { n ->
                        val id = "${w.key}:$n"
                        ExerciseRow(n, open == id) { open = if (open == id) null else id }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
                    Btn("Fazer o treino ${w.key}", Modifier.fillMaxWidth().padding(14.dp), ghost = true, size = 16.sp) {
                        Engine.start(ctx, w.key, lvl)
                    }
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.surface)
                    .border(1.dp, C.line, RoundedCornerShape(16.dp)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Title("Bloco final", 20.sp)
                    Body("Fecha todos os treinos, antes do alongamento", C.muted, 13.sp)
                }
                val id = "fim"
                ExerciseRow(Plan.FINISHER, open == id) { open = if (open == id) null else id }
            }
        }
        item {
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.width(3.dp).height(64.dp).background(C.signal))
                Spacer(Modifier.width(12.dp))
                Body("Se sentir dor no peito, tontura ou falta de ar fora do normal, pare e procure um médico. Dor nas articulações é sinal para usar a versão mais fácil.", C.muted, 13.sp)
            }
        }
    }
}

@Composable
fun ExerciseRow(name: String, open: Boolean, onToggle: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Body(name, size = 15.sp)
        Spacer(Modifier.weight(1f))
        Text(if (open) "–" else "+", color = C.muted, fontFamily = Display, fontSize = 22.sp)
    }
    if (open) ExerciseDetail(name)
}

@Composable
fun ExerciseDetail(name: String) {
    val ctx = LocalContext.current
    val ex = Plan.exercises.getValue(name)
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (Anims.map.containsKey(name)) {
            ExerciseDemo(name, demoColors,
                Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(12.dp)).background(C.bg))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ex.steps.forEachIndexed { i, s ->
                Row {
                    Text("${i + 1}", color = C.accent, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.width(22.dp))
                    Body(s, size = 15.sp)
                }
            }
        }
        Row {
            Box(Modifier.width(3.dp).height(40.dp).background(C.signal))
            Spacer(Modifier.width(10.dp))
            Text(buildLabel("Cuidado: ", ex.caution), fontSize = 14.sp, lineHeight = 20.sp)
        }
        Text(buildLabel("Mais fácil: ", ex.easier), fontSize = 14.sp, lineHeight = 20.sp)
        Btn("▶  Ver vídeos no YouTube", ghost = true, size = 15.sp) {
            val url = "https://www.youtube.com/results?search_query=" + Uri.encode("$name exercício como fazer")
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }
}

private fun buildLabel(label: String, text: String) = androidx.compose.ui.text.buildAnnotatedString {
    pushStyle(androidx.compose.ui.text.SpanStyle(color = C.fg, fontWeight = FontWeight.Bold)); append(label); pop()
    pushStyle(androidx.compose.ui.text.SpanStyle(color = C.muted)); append(text); pop()
}

/* ---------- Progresso ---------- */

@Composable
fun ProgressTab() {
    val focus = LocalFocusManager.current
    val log = Store.log.toList()
    val weights = Store.weights.toList()
    var input by rememberSaveable { mutableStateOf("") }
    val ws = Plan.weekStart(LocalDate.now()).toString()

    LazyColumn(contentPadding = listPad, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card {
                Title("Resumo", 20.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Stat("${log.count { !it.partial }}", "treinos feitos")
                    Stat("${log.count { it.date >= ws && !it.partial }}/3", "nesta semana")
                    Stat("${log.sumOf { it.minutes }}", "minutos no total")
                }
            }
        }
        item {
            Card {
                Title("Peso", 20.sp)
                val first = weights.first(); val last = weights.last()
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    Stat("${kg(last.kg)} kg", "atual")
                    if (weights.size > 1) {
                        val df = last.kg - first.kg
                        Stat((if (df > 0) "+" else "") + kg(df) + " kg", "desde ${fmtDay(first.date)}")
                    }
                }
                if (weights.size < 2) {
                    Body("Ponto de partida: ${kg(first.kg)} kg. Registre seu peso daqui a uma semana para ver a linha.", C.muted, 14.sp)
                } else WeightChart(weights)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input, onValueChange = { input = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6) },
                        placeholder = { Text("Peso de hoje (kg)", color = C.muted) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C.accent, unfocusedBorderColor = C.line,
                            focusedTextColor = C.fg, unfocusedTextColor = C.fg, cursorColor = C.accent),
                        modifier = Modifier.weight(1f),
                    )
                    Btn("Salvar", size = 15.sp) {
                        val v = input.replace(',', '.').toDoubleOrNull()
                        if (v != null && v in 30.0..300.0) { Store.addWeight(v); input = ""; focus.clearFocus() }
                    }
                }
                Body("Pese uma vez por semana, no mesmo dia e horário, de manhã. O peso oscila 1–2 kg de um dia para o outro.", C.muted, 13.sp)
            }
        }
        item {
            Card {
                Title("Histórico", 20.sp)
                if (log.isEmpty()) Body("Seus treinos aparecem aqui. Termine o primeiro e toque em Salvar.", C.muted, 14.sp)
                log.asReversed().take(40).forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(99.dp)).background(C.accent).padding(horizontal = 8.dp, vertical = 1.dp)) {
                            Text(e.workout, color = C.ink, fontWeight = FontWeight.Bold, fontFamily = Display, fontSize = 14.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Body(fmtDay(e.date) + if (e.partial) " (parcial)" else "", size = 14.sp)
                            Text("${e.minutes} min · nível ${e.level}" + (e.rpe?.let { " · nota $it" } ?: ""), color = C.muted, fontSize = 12.sp, style = Tnum)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightChart(ws: List<WeightEntry>) {
    val vals = ws.map { it.kg }
    val mn = floor(vals.min() - 1); val mx = ceil(vals.max() + 1)
    Row(Modifier.fillMaxWidth().height(120.dp)) {
        Column(Modifier.padding(end = 6.dp).height(120.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("${mx.toInt()}", color = C.muted, fontSize = 11.sp, style = Tnum)
            Text("${mn.toInt()}", color = C.muted, fontSize = 11.sp, style = Tnum)
        }
        Canvas(Modifier.weight(1f).height(120.dp)) {
            val pad = 6f
            fun x(i: Int) = pad + (size.width - 2 * pad) * i / (ws.size - 1)
            fun y(v: Double) = (pad + (size.height - 2 * pad) * (1 - (v - mn) / (mx - mn))).toFloat()
            drawLine(C.line, Offset(0f, y(mx)), Offset(size.width, y(mx)), 2f)
            drawLine(C.line, Offset(0f, y(mn)), Offset(size.width, y(mn)), 2f)
            val path = Path(); val area = Path()
            ws.forEachIndexed { i, w ->
                if (i == 0) { path.moveTo(x(i), y(w.kg)); area.moveTo(x(i), size.height); area.lineTo(x(i), y(w.kg)) }
                else { path.lineTo(x(i), y(w.kg)); area.lineTo(x(i), y(w.kg)) }
            }
            area.lineTo(x(ws.size - 1), size.height); area.close()
            drawPath(area, C.accent.copy(alpha = 0.12f))
            drawPath(path, C.accent, style = Stroke(width = 5f, cap = StrokeCap.Round))
            drawCircle(C.accent, 9f, Offset(x(ws.size - 1), y(ws.last().kg)))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        Text(fmtDay(ws.first().date), color = C.muted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text(fmtDay(ws.last().date), color = C.muted, fontSize = 11.sp)
    }
}

/* ---------- Treino em andamento ---------- */

@Composable
fun PlayerScreen(ps: PlayerState) {
    var quitting by remember { mutableStateOf(false) }
    var rpe by remember { mutableIntStateOf(3) }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    BackHandler(enabled = !ps.finished) { Engine.pause(); quitting = true }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row {
            Eyebrow("${ps.index + 1} / ${ps.steps.size}")
            Spacer(Modifier.weight(1f))
            Eyebrow("${ceil(ps.leftMs / 60000.0).toInt()} min restantes")
        }
        val frac = if (ps.totalMs > 0) (1f - ps.leftMs.toFloat() / ps.totalMs).coerceIn(0f, 1f) else 0f
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)).background(C.line)) {
            Box(Modifier.fillMaxWidth(frac).height(6.dp).background(C.accent))
        }
        when {
            ps.finished -> FinishedPane(ps, rpe, { rpe = it })
            quitting -> QuitPane(ps) { quitting = false; Engine.togglePause() }
            else -> RunPane(ps) { Engine.pause(); quitting = true }
        }
    }
}

@Composable
fun RunPane(ps: PlayerState, onQuit: () -> Unit) {
    val step = ps.step ?: return
    val isRest = step.kind == StepKind.REST
    val nextWork = ps.steps.drop(ps.index + 1).firstOrNull { it.kind != StepKind.REST }
    val demoName = if (isRest) nextWork?.name else step.name
    val tint = if (isRest) C.rest else C.accent

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.clip(RoundedCornerShape(99.dp)).background(tint).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(step.phase.uppercase(), color = C.bg, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.8.sp)
        }
        if (demoName != null && Anims.map.containsKey(demoName)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(C.surface).border(1.dp, C.line, RoundedCornerShape(12.dp))) {
                ExerciseDemo(demoName, demoColors, Modifier.fillMaxSize())
                if (isRest) Box(Modifier.padding(10.dp)) { Eyebrow("A seguir: $demoName") }
            }
        }
        Title(step.name, 32.sp, align = TextAlign.Center)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            val p = if (step.seconds > 0) (1f - ps.remainingMs / (step.seconds * 1000f)).coerceIn(0f, 1f) else 0f
            Canvas(Modifier.size(84.dp)) {
                val sw = 10.dp.toPx()
                val tl = Offset(sw / 2, sw / 2); val sz = Size(size.width - sw, size.height - sw)
                drawArc(C.line, 0f, 360f, false, tl, sz, style = Stroke(sw))
                drawArc(tint, -90f, 360f * p, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
            Text(clock(ps.remainingMs), color = C.fg, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 72.sp, style = Tnum)
        }
        if (step.hint.isNotEmpty()) Body(step.hint, C.muted, 15.sp, TextAlign.Center)
        Text(if (nextWork != null) "A seguir: ${nextWork.name}" else "Último exercício", color = C.muted, fontSize = 14.sp)
    }
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn("Voltar", Modifier.weight(1f), ghost = true, size = 16.sp) { Engine.prev() }
        Btn(if (ps.paused) "Seguir" else "Pausar", Modifier.weight(1.4f), size = 16.sp) { Engine.togglePause() }
        Btn("Pular", Modifier.weight(1f), ghost = true, size = 16.sp) { Engine.next() }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("Encerrar", color = C.muted, fontSize = 15.sp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onQuit).padding(10.dp))
    }
}

@Composable
fun QuitPane(ps: PlayerState, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Title("Encerrar o treino?", 30.sp, align = TextAlign.Center)
        Body("Você fez ${(ps.elapsedMs / 60000.0).let { Math.round(it) }} min. Pode salvar como treino parcial ou sair sem salvar.", C.muted, align = TextAlign.Center)
        Btn("Salvar parcial", Modifier.fillMaxWidth()) { Engine.save(null, partial = true); Engine.close() }
        Btn("Sair sem salvar", Modifier.fillMaxWidth(), ghost = true) { Engine.close() }
        Text("Voltar ao treino", color = C.muted, fontSize = 15.sp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(10.dp))
    }
}

@Composable
fun FinishedPane(ps: PlayerState, rpe: Int, setRpe: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.clip(RoundedCornerShape(99.dp)).background(C.accent).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text("TREINO CONCLUÍDO", color = C.ink, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.8.sp)
        }
        Title("Treino ${ps.workout} feito", 34.sp, align = TextAlign.Center)
        Body("${Math.round(ps.spentMs / 60000.0)} minutos · nível ${ps.level}", C.muted)
        Body("Quão difícil foi?", size = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Muito fácil", "Fácil", "Bom", "Difícil", "Pesado").forEachIndexed { i, t ->
                Choice(rpe == i + 1, Modifier.weight(1f), onClick = { setRpe(i + 1) }) {
                    Text("${i + 1}", color = C.fg, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text(t, color = C.muted, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
        Btn("Salvar treino", Modifier.fillMaxWidth()) { Engine.save(rpe, partial = false); Engine.close() }
    }
}
