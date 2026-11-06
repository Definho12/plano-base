package com.guilherme.planobase

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

data class Exercise(val name: String, val hint: String, val steps: List<String>, val caution: String, val easier: String)
data class Level(val work: Int, val rest: Int, val rounds: Int, val finisher: Int, val label: String)
data class Workout(val key: String, val name: String, val focus: String, val exercises: List<String>)
enum class StepKind { WARM, WORK, REST, COOL }
data class Step(val phase: String, val name: String, val seconds: Int, val hint: String, val kind: StepKind)
data class Simple(val name: String, val seconds: Int, val hint: String)

data class LogEntry(val date: String, val workout: String, val level: Int, val minutes: Int, val rpe: Int?, val partial: Boolean)
data class WeightEntry(val date: String, val kg: Double, val start: Boolean = false)
/** days: 1 = segunda ... 7 = domingo */
data class ReminderCfg(val enabled: Boolean, val hour: Int, val minute: Int, val days: Set<Int>)

object Plan {
    const val ROUND_REST = 60
    const val FINISHER = "Marcha acelerada"

    val levels = mapOf(
        1 to Level(30, 30, 3, 180, "Semanas 1–2"),
        2 to Level(40, 20, 3, 300, "Semanas 3–5"),
        3 to Level(45, 15, 4, 420, "Semana 6+"),
    )

    val exercises: Map<String, Exercise> = listOf(
        Exercise("Agachamento na cadeira", "Encoste de leve na cadeira e levante sem usar as mãos.",
            listOf("Fique de pé na frente da cadeira, pés na largura do quadril.", "Leve o quadril para trás e desça até encostar de leve no assento.", "Empurre o chão com os calcanhares e suba, apertando o glúteo no alto."),
            "Os joelhos não podem cair para dentro. Aponte-os na direção dos pés.", "Use uma cadeira mais alta ou apoie as mãos nas coxas para subir."),
        Exercise("Ponte de glúteo", "Suba o quadril, aperte o glúteo 1 s e desça devagar.",
            listOf("Deite de costas, joelhos dobrados e pés no chão perto do quadril.", "Aperte o glúteo e suba o quadril até formar uma linha dos joelhos aos ombros.", "Segure 1 s em cima e desça devagar."),
            "Não arqueie a lombar no alto. Quem faz força é o glúteo.", "Suba só metade do caminho."),
        Exercise("Avanço para trás com apoio", "Segure na cadeira, passo para trás, desça o joelho de trás. Alterne.",
            listOf("Fique ao lado do encosto da cadeira e segure nele.", "Dê um passo grande para trás e desça o joelho de trás em direção ao chão.", "Volte empurrando com a perna da frente. Alterne as pernas."),
            "O joelho da frente fica acima do tornozelo, sem passar muito da ponta do pé.", "Passo curto e descida pequena."),
        Exercise("Prancha com joelhos", "Antebraços e joelhos no chão, corpo reto, abdômen firme.",
            listOf("Apoie os antebraços no chão, cotovelos embaixo dos ombros.", "Com os joelhos no chão, alinhe o corpo dos joelhos até a cabeça.", "Contraia o abdômen e respire normalmente."),
            "Não deixe o quadril subir nem a barriga afundar.", "Faça em pé, com os antebraços na parede."),
        Exercise("Elevação de panturrilha", "Suba na ponta dos pés em 1 s, desça em 2 s.",
            listOf("De pé, apoie as mãos na parede.", "Suba na ponta dos pés em 1 s.", "Desça em 2 s, controlando."),
            "Suba reto, sem deixar o tornozelo virar para fora.", "Suba só até a metade."),
        Exercise("Marcha com joelho alto", "Joelhos até a altura do quadril, ritmo forte, sem pular.",
            listOf("Fique de pé, tronco reto.", "Suba um joelho até a altura do quadril e troque, sem pular.", "Balance os braços no ritmo das pernas."),
            "Não incline o tronco para trás para subir o joelho.", "Ritmo moderado, joelho mais baixo."),
        Exercise("Flexão inclinada", "Mãos na mesa, corpo reto, peito vai até a mesa e volta.",
            listOf("Apoie as mãos na beira de uma mesa firme, um pouco mais abertas que os ombros.", "Afaste os pés até o corpo ficar reto, apoiado na ponta dos pés.", "Dobre os cotovelos e leve o peito até perto da mesa. Empurre de volta."),
            "Confira se a mesa não escorrega. Os cotovelos vão para trás, não para os lados.", "Faça na parede."),
        Exercise("Super-homem", "De barriga para baixo, eleve braços e pernas e segure 2 s.",
            listOf("Deite de barriga para baixo, braços estendidos à frente.", "Eleve braços, peito e pernas alguns centímetros do chão.", "Segure 2 s e desça devagar."),
            "Olhe para o chão, sem jogar a cabeça para trás.", "Eleve um braço e a perna oposta, alternando."),
        Exercise("Boxe no ar", "Socos alternados rápidos, mãos voltam para o rosto.",
            listOf("Pés afastados, um à frente do outro, joelhos levemente dobrados.", "Mãos protegendo o rosto.", "Soque à frente alternando os braços e volte a mão para o rosto."),
            "Não trave o cotovelo no fim do soco.", "Socos mais lentos."),
        Exercise("Dead bug", "Estenda braço e perna opostos sem tirar a lombar do chão.",
            listOf("Deite de costas, braços para o teto e joelhos dobrados a 90° no ar.", "Estenda devagar um braço para trás e a perna oposta para frente.", "Volte e troque o lado."),
            "A lombar fica colada no chão o tempo todo. Se ela descolar, diminua o movimento.", "Mova só as pernas, uma de cada vez."),
        Exercise("Toque no ombro inclinado", "Na posição de flexão na mesa, toque o ombro oposto, alternando.",
            listOf("Fique na posição de flexão inclinada, braços estendidos na mesa.", "Tire uma mão e toque o ombro oposto.", "Volte a mão e troque. O quadril fica parado."),
            "Se o corpo balançar muito, afaste mais os pés.", "Faça na parede."),
        Exercise("Polichinelo sem salto", "Passo para o lado enquanto os braços sobem. Alterne.",
            listOf("De pé, braços ao lado do corpo.", "Dê um passo para o lado enquanto sobe os braços acima da cabeça.", "Volte ao centro e repita para o outro lado."),
            "Mantenha os joelhos levemente soltos.", "Braços sobem só até a altura dos ombros."),
        Exercise("Agachamento com braços", "Agache até a cadeira e, ao subir, estique os braços para cima.",
            listOf("Fique de pé na frente da cadeira.", "Agache até encostar de leve no assento, braços à frente.", "Ao subir, estique os braços acima da cabeça."),
            "Os joelhos não podem cair para dentro.", "Agachamento curto, braços só até a altura dos ombros."),
        Exercise("Patinador sem salto", "Passo largo para o lado, cruze a outra perna atrás. Alterne.",
            listOf("De pé, joelhos levemente dobrados.", "Dê um passo largo para o lado e cruze a outra perna por trás.", "Balance os braços e repita para o outro lado."),
            "Passo controlado. Não precisa ser rápido no começo.", "Passo menor, sem cruzar a perna."),
        Exercise("Escalador na mesa", "Mãos na mesa, traga os joelhos ao peito alternando.",
            listOf("Apoie as mãos na beira da mesa, corpo reto e inclinado.", "Traga um joelho em direção ao peito.", "Volte e troque, em ritmo constante."),
            "Mantenha os ombros acima das mãos.", "Mais devagar, ou com as mãos na parede."),
        Exercise("Passo lateral com soco", "Passo para o lado e soco cruzado. Volte e troque.",
            listOf("De pé, mãos protegendo o rosto.", "Dê um passo para o lado e solte um soco cruzado com o braço oposto.", "Volte ao centro e repita para o outro lado."),
            "Gire o pé de trás junto com o soco para proteger o joelho.", "Só o passo, sem o soco."),
        Exercise(FINISHER, "Marcha forte no lugar. Dá para falar frases curtas, mas não cantar.",
            listOf("Marche no lugar em ritmo forte.", "Braços ativos, acompanhando as pernas.", "Mantenha o ritmo até o fim do bloco."),
            "Se faltar ar para falar, diminua o ritmo.", "Marcha normal."),
    ).associateBy { it.name }

    val warmup = listOf(
        Simple("Marcha no lugar", 60, "Joelhos até a altura do quadril, braços acompanhando."),
        Simple("Círculos de braço", 30, "15 s para frente, 15 s para trás. Círculos amplos."),
        Simple("Rotação de quadril", 30, "Mãos na cintura, círculos grandes nos dois sentidos."),
        Simple("Inclinação lateral", 30, "Braço por cima da cabeça, incline para o lado. Alterne."),
        Simple("Agachamento curto", 45, "Desça só até onde for confortável, devagar."),
        Simple("Polichinelo sem salto", 60, "Um pé vai para o lado enquanto os braços sobem. Alterne."),
        Simple("Toque cruzado", 45, "Em pé, leve o joelho até a mão oposta, alternando."),
    )
    val cooldown = listOf(
        Simple("Posterior da coxa", 40, "Sentado, uma perna estendida. Incline o tronco até sentir a parte de trás da coxa. Troque na metade."),
        Simple("Quadríceps em pé", 40, "Mão na parede, segure o pé atrás do corpo. Troque na metade."),
        Simple("Peitoral na parede", 40, "Antebraço na parede, gire o corpo para o lado oposto. Troque na metade."),
        Simple("Gato e vaca", 40, "De quatro apoios, arredonde e arqueie as costas devagar."),
        Simple("Joelhos ao peito", 40, "Deitado de costas, abrace os joelhos e balance de leve."),
        Simple("Respiração", 40, "Inspire 4 s pelo nariz, solte 6 s pela boca."),
    )

    val workouts = listOf(
        Workout("A", "Pernas e core", "Força de pernas e abdômen",
            listOf("Agachamento na cadeira", "Ponte de glúteo", "Avanço para trás com apoio", "Prancha com joelhos", "Elevação de panturrilha", "Marcha com joelho alto")),
        Workout("B", "Superior e core", "Peito, costas, braços e abdômen",
            listOf("Flexão inclinada", "Super-homem", "Boxe no ar", "Dead bug", "Toque no ombro inclinado", "Polichinelo sem salto")),
        Workout("C", "Cardio leve", "Condicionamento e gasto calórico",
            listOf("Polichinelo sem salto", "Agachamento com braços", "Patinador sem salto", "Escalador na mesa", "Passo lateral com soco", "Marcha com joelho alto")),
    )
    val order = listOf("A", "B", "C")
    /** dia da semana (1 = segunda) -> treino sugerido */
    val planDay = mapOf(1 to "A", 3 to "B", 5 to "C")

    fun workout(key: String) = workouts.first { it.key == key }

    fun buildSteps(key: String, level: Int): List<Step> {
        val l = levels.getValue(level)
        val w = workout(key)
        val st = mutableListOf<Step>()
        warmup.forEach { st += Step("Aquecimento", it.name, it.seconds, it.hint, StepKind.WARM) }
        for (r in 1..l.rounds) {
            w.exercises.forEachIndexed { i, n ->
                st += Step("Rodada $r de ${l.rounds}", n, l.work, exercises.getValue(n).hint, StepKind.WORK)
                val last = i == w.exercises.lastIndex
                if (!last) st += Step("Descanso", "Respire", l.rest, "", StepKind.REST)
                else if (r < l.rounds) st += Step("Descanso", "Pausa entre rodadas", ROUND_REST, "Beba água. Ande devagar pela sala.", StepKind.REST)
            }
        }
        st += Step("Descanso", "Prepare-se", 30, "Bloco final de cardio.", StepKind.REST)
        st += Step("Bloco final", FINISHER, l.finisher, exercises.getValue(FINISHER).hint, StepKind.WORK)
        cooldown.forEach { st += Step("Alongamento", it.name, it.seconds, it.hint, StepKind.COOL) }
        return st
    }

    fun totalMinutes(key: String, level: Int) = (buildSteps(key, level).sumOf { it.seconds } / 60.0).roundToInt()

    fun nextKey(log: List<LogEntry>): String {
        val last = log.lastOrNull { !it.partial } ?: log.lastOrNull() ?: return "A"
        return order[(order.indexOf(last.workout) + 1) % order.size]
    }

    fun weekStart(today: LocalDate): LocalDate = today.minusDays((today.dayOfWeek.value - 1).toLong())

    /** Próximo horário de lembrete depois de [now], ou null se não houver dias marcados. */
    fun nextReminder(cfg: ReminderCfg, now: LocalDateTime): LocalDateTime? {
        if (cfg.days.isEmpty()) return null
        for (d in 0..7L) {
            val date = now.toLocalDate().plusDays(d)
            if (date.dayOfWeek.value in cfg.days) {
                val t = date.atTime(cfg.hour, cfg.minute)
                if (t.isAfter(now)) return t
            }
        }
        return null
    }

    fun levelHint(level: Int, log: List<LogEntry>): String {
        val at = log.filter { it.level == level && !it.partial }
        val recent = at.takeLast(3)
        val avg = if (recent.isEmpty()) 0.0 else recent.map { (it.rpe ?: 3).toDouble() }.average()
        return when {
            level == 3 -> "Nível máximo deste plano. Para progredir, aumente a caminhada nos outros dias."
            at.size >= 6 && avg <= 3.0 -> "Você fez ${at.size} treinos neste nível e os últimos foram tranquilos. Pode subir para o nível ${level + 1}."
            at.isNotEmpty() -> "${at.size} de 6 treinos neste nível. Suba quando os últimos parecerem fáceis (nota 3 ou menos)."
            else -> "Comece no nível 1, mesmo que pareça fácil. As articulações se adaptam mais devagar que o fôlego."
        }
    }
}
