package com.guilherme.planobase

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Ponto no "mundo" do boneco: x para a frente, y para cima, chão em y = 0. */
data class Pt(val x: Float, val y: Float) {
    fun lerp(o: Pt, k: Float) = Pt(x + (o.x - x) * k, y + (o.y - y) * k)
}
private fun p(x: Number, y: Number) = Pt(x.toFloat(), y.toFloat())

/** Pose: quadril, inclinação do tronco (graus), pés e mãos (alvos), lado de dobra do joelho. */
data class Pose(
    val h: Pt = p(0, 76), val t: Float = 2f,
    val nf: Pt = p(3, 0), val ff: Pt = p(3, 0),
    val nh: Pt = p(4, 74), val fh: Pt = p(4, 74),
    val kb: Float = 1f, val nt: Pt? = null, val ft: Pt? = null,
) {
    fun lerp(o: Pose, k: Float) = Pose(
        h.lerp(o.h, k), t + (o.t - t) * k, nf.lerp(o.nf, k), ff.lerp(o.ff, k), nh.lerp(o.nh, k), fh.lerp(o.fh, k),
        kb, if (nt != null && o.nt != null) nt.lerp(o.nt, k) else nt, if (ft != null && o.ft != null) ft.lerp(o.ft, k) else ft)
}

data class Props(val chairX: Float? = null, val tableX: Float? = null, val tableH: Float = 59f, val wallX: Float? = null)
data class Anim(val dur: Int, val frames: List<Pose>, val front: Boolean = false, val props: Props = Props())

object Anims {
    private val ST = Pose()
    private val PUSH_TOP = Pose(h = p(1, 56.5), t = 42f, nf = p(-50, 0), ff = p(-50, 0), nh = p(72, 59), fh = p(72, 59))
    private val PUSH_BOT = PUSH_TOP.copy(h = p(11.5, 44.7), t = 54f)
    private val TABLE = Props(tableX = 66f)
    private val CHAIR = Props(chairX = -54f)
    private val SQ_UP = Pose(t = 4f, nf = p(8, 0), ff = p(8, 0), nh = p(52, 116), fh = p(54, 118))
    private val SQ_DN = Pose(h = p(-22, 50), t = 40f, nf = p(8, 0), ff = p(8, 0), nh = p(56, 96), fh = p(58, 98))
    private val KNEE_A = Pose(nf = p(18, 38), ff = p(2, 0), nh = p(-18, 86), fh = p(22, 98))
    private val KNEE_B = Pose(nf = p(2, 0), ff = p(18, 38), nh = p(22, 98), fh = p(-18, 86))
    private val JACK0 = Pose(t = 0f, nf = p(-8, 0), ff = p(8, 0), nh = p(-18, 74), fh = p(18, 74))
    private val GUARD = Pose(h = p(0, 72), t = 6f, nf = p(16, 0), ff = p(-12, 0), nh = p(22, 122), fh = p(18, 118))
    private val PUNCH_F = Pose(h = p(0, 74), t = 0f, nf = p(-8, 0), ff = p(8, 0), nh = p(-8, 122), fh = p(8, 122))
    private val LUNGE_UP = Pose(nh = p(40, 88), fh = p(4, 74), nf = p(4, 0), ff = p(4, 0))
    private val BUG0 = Pose(h = p(0, 9), t = 90f, nf = p(-38, 46), ff = p(-38, 46), nh = p(50, 62), fh = p(50, 62), kb = -1f)
    private val PLANK = Pose(h = p(0, 24), t = 76f, nf = p(-66, 5), ff = p(-66, 5), nh = p(76, 3), fh = p(76, 3))

    val map: Map<String, Anim> = mapOf(
        "Agachamento na cadeira" to Anim(1100, listOf(SQ_UP, SQ_DN), props = CHAIR),
        "Agachamento curto" to Anim(1000, listOf(SQ_UP, Pose(h = p(-12, 62), t = 24f, nf = p(8, 0), ff = p(8, 0), nh = p(56, 104), fh = p(58, 106)))),
        "Agachamento com braços" to Anim(1100, listOf(Pose(nf = p(8, 0), ff = p(8, 0), nh = p(8, 180), fh = p(12, 180)), SQ_DN), props = CHAIR),
        "Ponte de glúteo" to Anim(1200, listOf(
            Pose(h = p(0, 9), t = 90f, nf = p(-30, 0), ff = p(-30, 0), nh = p(-4, 3), fh = p(-4, 3), kb = -1f),
            Pose(h = p(0, 34), t = 116f, nf = p(-30, 0), ff = p(-30, 0), nh = p(-2, 3), fh = p(-2, 3), kb = -1f))),
        "Avanço para trás com apoio" to Anim(1000, listOf(
            LUNGE_UP,
            Pose(h = p(-10, 48), t = 8f, nf = p(-60, 6), ff = p(12, 0), nh = p(40, 88), fh = p(-2, 58)),
            LUNGE_UP,
            Pose(h = p(-10, 48), t = 8f, nf = p(12, 0), ff = p(-60, 6), nh = p(40, 88), fh = p(-2, 58))),
            props = Props(chairX = 40f)),
        "Prancha com joelhos" to Anim(1600, listOf(PLANK, PLANK.copy(h = p(0, 26)))),
        "Elevação de panturrilha" to Anim(1200, listOf(
            Pose(t = 4f, nf = p(4, 0), ff = p(4, 0), nh = p(46, 118), fh = p(46, 116), nt = p(13, 0), ft = p(13, 0)),
            Pose(h = p(0, 84), t = 4f, nf = p(4, 8), ff = p(4, 8), nh = p(46, 124), fh = p(46, 122), nt = p(13, 0), ft = p(13, 0))),
            props = Props(wallX = 46f)),
        "Marcha com joelho alto" to Anim(420, listOf(KNEE_A, ST, KNEE_B, ST)),
        "Marcha no lugar" to Anim(520, listOf(KNEE_A, ST, KNEE_B, ST)),
        "Marcha acelerada" to Anim(300, listOf(KNEE_A, ST, KNEE_B, ST)),
        "Flexão inclinada" to Anim(1300, listOf(PUSH_TOP, PUSH_BOT), props = TABLE),
        "Toque no ombro inclinado" to Anim(650, listOf(PUSH_TOP, PUSH_TOP.copy(nh = p(40, 98)), PUSH_TOP, PUSH_TOP.copy(fh = p(40, 98))), props = TABLE),
        "Escalador na mesa" to Anim(420, listOf(PUSH_TOP, PUSH_TOP.copy(nf = p(-14, 24)), PUSH_TOP, PUSH_TOP.copy(ff = p(-14, 24))), props = TABLE),
        "Super-homem" to Anim(1300, listOf(
            Pose(h = p(0, 8), t = 90f, nf = p(-74, 4), ff = p(-74, 4), nh = p(106, 8), fh = p(106, 8)),
            Pose(h = p(0, 8), t = 80f, nf = p(-72, 14), ff = p(-72, 14), nh = p(100, 26), fh = p(100, 26)))),
        "Boxe no ar" to Anim(280, listOf(
            GUARD, GUARD.copy(h = p(2, 72), t = 8f, nh = p(60, 122)),
            GUARD, GUARD.copy(h = p(2, 72), t = 8f, fh = p(60, 120)))),
        "Dead bug" to Anim(1100, listOf(
            BUG0, BUG0.copy(nf = p(-74, 12), fh = p(104, 14)), BUG0, BUG0.copy(ff = p(-74, 12), nh = p(104, 14)))),
        "Polichinelo sem salto" to Anim(550, listOf(
            JACK0, Pose(h = p(-6, 71), t = 0f, nf = p(-38, 0), ff = p(8, 0), nh = p(-34, 176), fh = p(34, 176)),
            JACK0, Pose(h = p(6, 71), t = 0f, nf = p(-8, 0), ff = p(38, 0), nh = p(-34, 176), fh = p(34, 176))), front = true),
        "Patinador sem salto" to Anim(800, listOf(
            Pose(h = p(-26, 66), t = -4f, nf = p(-46, 0), ff = p(-12, 5), nh = p(2, 96), fh = p(34, 102)),
            Pose(h = p(26, 66), t = 4f, nf = p(12, 5), ff = p(46, 0), nh = p(-34, 102), fh = p(-2, 96))), front = true),
        "Passo lateral com soco" to Anim(520, listOf(
            PUNCH_F, Pose(h = p(-22, 72), t = -3f, nf = p(-42, 0), ff = p(-4, 0), nh = p(-28, 116), fh = p(-62, 124)),
            PUNCH_F, Pose(h = p(22, 72), t = 3f, nf = p(4, 0), ff = p(42, 0), nh = p(62, 124), fh = p(28, 116))), front = true),
    )
}

private fun ik(b: Pt, t: Pt, a: Float, c: Float, s: Float): Pair<Pt, Pt> {
    var dx = t.x - b.x; var dy = t.y - b.y; var d = hypot(dx, dy)
    if (d < 0.001f) { dx = 0f; dy = -1f; d = 1f }
    val ux = dx / d; val uy = dy / d
    val dd = min(max(d, abs(a - c) + 0.01f), a + c - 0.01f)
    val ang = acos(((a * a + dd * dd - c * c) / (2 * a * dd)).coerceIn(-1f, 1f)) * s
    val jx = ux * cos(ang) - uy * sin(ang); val jy = ux * sin(ang) + uy * cos(ang)
    val j = Pt(b.x + a * jx, b.y + a * jy)
    val ex = t.x - j.x; val ey = t.y - j.y; val el = hypot(ex, ey).takeIf { it > 0f } ?: 1f
    return j to Pt(j.x + c * ex / el, j.y + c * ey / el)
}

data class DemoColors(val body: Color, val far: Color, val head: Color, val prop: Color, val floor: Color)

@Composable
fun ExerciseDemo(name: String, colors: DemoColors, modifier: Modifier = Modifier) {
    val anim = Anims.map[name] ?: return
    val time by produceState(0L, name) {
        val start = withFrameMillis { it }
        while (true) value = withFrameMillis { it } - start
    }
    Canvas(modifier) { drawAnim(anim, time, colors) }
}

private fun DrawScope.drawAnim(anim: Anim, time: Long, c: DemoColors) {
    val bx0 = if (anim.front) -105f else -100f
    val bx1 = if (anim.front) 105f else 130f
    val by0 = -8f; val by1 = 196f
    val s = min(size.width / (bx1 - bx0), size.height / (by1 - by0))
    val ox = (size.width - (bx1 - bx0) * s) / 2 - bx0 * s
    val oy = size.height - (size.height - (by1 - by0) * s) / 2 + by0 * s
    fun o(q: Pt) = Offset(ox + q.x * s, oy - q.y * s)
    fun line(a: Pt, b: Pt, col: Color, w: Float) = drawLine(col, o(a), o(b), w * s, StrokeCap.Round)

    val n = anim.frames.size
    val tt = time % (n.toLong() * anim.dur)
    val i = (tt / anim.dur).toInt()
    val k0 = (tt % anim.dur).toFloat() / anim.dur
    val k = ((1 - cos(k0 * PI)) / 2).toFloat()
    val q = anim.frames[i].lerp(anim.frames[(i + 1) % n], k)

    line(p(bx0 + 6, -2), p(bx1 - 6, -2), c.floor, 2.5f)
    anim.props.chairX?.let { x -> line(p(x, 44), p(x + 32, 44), c.prop, 5f); line(p(x, 0), p(x, 92), c.prop, 4f); line(p(x + 32, 0), p(x + 32, 44), c.prop, 4f) }
    anim.props.tableX?.let { x -> val h = anim.props.tableH; line(p(x, h), p(x + 46, h), c.prop, 6f); line(p(x + 5, 0), p(x + 5, h), c.prop, 4f); line(p(x + 41, 0), p(x + 41, h), c.prop, 4f) }
    anim.props.wallX?.let { x -> line(p(x, 0), p(x, 190), c.prop, 6f) }

    val rad = q.t * PI.toFloat() / 180f
    val td = Pt(sin(rad), cos(rad))
    val hip = q.h
    val sh = Pt(hip.x + 50 * td.x, hip.y + 50 * td.y)
    val head = Pt(hip.x + 64 * td.x, hip.y + 64 * td.y)
    val fo = if (anim.front) 1f else 0f
    val hipN = Pt(hip.x - 7 * fo, hip.y); val hipF = Pt(hip.x + 7 * fo, hip.y)
    val shN = Pt(sh.x - 13 * fo, sh.y); val shF = Pt(sh.x + 13 * fo, sh.y)

    fun leg(hp: Pt, foot: Pt, toe: Pt?, col: Color, side: Float) {
        val (kn, an) = ik(hp, foot, 38f, 38f, q.kb)
        line(hp, kn, col, 6f); line(kn, an, col, 6f)
        val tp = toe ?: if (anim.front) Pt(an.x + 5 * side, an.y - 1) else {
            val vx = an.x - kn.x; val vy = an.y - kn.y; val l = hypot(vx, vy).takeIf { it > 0f } ?: 1f
            Pt(an.x - vy / l * 9, an.y + vx / l * 9)
        }
        line(an, tp, col, 5f)
    }
    fun arm(sp: Pt, hand: Pt, col: Color) {
        val (el, hd) = ik(sp, hand, 28f, 26f, -1f)
        line(sp, el, col, 5.5f); line(el, hd, col, 5.5f)
        drawCircle(col, 3.6f * s, o(hd))
    }
    val far = if (anim.front) c.body else c.far
    leg(hipF, q.ff, q.ft, far, 1f); arm(shF, q.fh, far)
    line(hip, sh, c.body, 8f)
    if (anim.front) { line(hipN, hipF, c.body, 7f); line(shN, shF, c.body, 7f) }
    drawCircle(c.head, 9.5f * s, o(head))
    leg(hipN, q.nf, q.nt, c.body, -1f); arm(shN, q.nh, c.body)
}
