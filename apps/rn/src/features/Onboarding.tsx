import { ScrollView, StyleSheet, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { CampoNumero, Cta, Escolha } from "../components/ui";
import { useDia } from "../data/day";
import { tokens } from "../theme/tokens";

const DIAS = ["Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"];

export function TelaTeto() {
  const { dia, patch } = useDia();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  return (
    <ScrollView style={estilos.tela} contentContainerStyle={[estilos.conteudo, { paddingTop: insets.top + 16 }]}>
      <Text style={estilos.titulo} accessibilityLabel="Qual é o teto de kcal?">
        Qual é o teto de kcal?
      </Text>
      <Text style={estilos.muted}>Você manda no número.</Text>
      <Escolha titulo="Mesmo todos os dias" ativo={dia.modo === "mesmo"} onPress={() => patch({ modo: "mesmo" })} />
      <Escolha titulo="Útil / fds" ativo={dia.modo === "util"} onPress={() => patch({ modo: "util" })} />
      <Escolha titulo="7 dias" ativo={dia.modo === "sete"} onPress={() => patch({ modo: "sete" })} />
      {dia.modo === "util" ? (
        <View style={estilos.gap}>
          <CampoNumero label="Seg–sex" valor={dia.campoUtil} onChange={(campoUtil) => patch({ campoUtil })} />
          <CampoNumero label="Sáb–dom" valor={dia.campoFds} onChange={(campoFds) => patch({ campoFds })} />
        </View>
      ) : null}
      {dia.modo === "sete"
        ? DIAS.map((nome, i) => (
            <CampoNumero
              key={nome}
              label={nome}
              valor={dia.camposDia[i] ?? ""}
              onChange={(valor) => {
                const camposDia = [...dia.camposDia];
                camposDia[i] = valor;
                patch({ camposDia });
              }}
            />
          ))
        : null}
      {dia.modo === "mesmo" ? (
        <CampoNumero label="Teto" valor={dia.kcalUnico} onChange={(kcalUnico) => patch({ kcalUnico })} />
      ) : null}
      <Text style={estilos.dim}>Dá pra mudar o número depois. Isso não é cálculo de nutricionista.</Text>
      <Cta label="Continuar" onPress={() => router.push("/onboarding/treino")} />
    </ScrollView>
  );
}

export function TelaEat() {
  const { dia, patch } = useDia();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const pct = Number(dia.pct) || 0;
  const extra = Math.trunc((480 * pct) / 100);
  return (
    <ScrollView style={estilos.tela} contentContainerStyle={[estilos.conteudo, { paddingTop: insets.top + 16 }]}>
      <Text style={estilos.kicker}>PRIMEIRO OPEN</Text>
      <Text style={estilos.titulo}>Quando você treina, o teto sobe?</Text>
      <Escolha
        titulo="Não entra · 0%"
        corpo="Treino 1000 e o teto continua o mesmo."
        ativo={dia.eat === "zero"}
        onPress={() => patch({ eat: "zero" })}
      />
      <Escolha
        titulo="Entra um pouco"
        corpo="Você escolhe a %."
        ativo={dia.eat === "parcial"}
        onPress={() => patch({ eat: "parcial" })}
      />
      {dia.eat === "parcial" ? (
        <View style={estilos.gap}>
          <CampoNumero label="% que entra" valor={dia.pct} suffix="%" onChange={(valor) => patch({ pct: valor })} />
          <Text style={estilos.gold}>Preview: treino 480 → teto +{extra}. Crédito real só com treino anotado.</Text>
        </View>
      ) : null}
      <Escolha
        titulo="Entra tudo · 100%"
        corpo="Treino 480 → teto +480."
        ativo={dia.eat === "cem"}
        onPress={() => patch({ eat: "cem" })}
      />
      <Text style={estilos.dim}>estimativa, não consulta</Text>
      <Cta
        label="Entrar no app"
        onPress={() => {
          patch({ onboardingFeito: true });
          router.replace("/");
        }}
      />
    </ScrollView>
  );
}

const estilos = StyleSheet.create({
  tela: { flex: 1, backgroundColor: tokens.bg },
  conteudo: { padding: 20, gap: 12, paddingBottom: 40 },
  titulo: { color: tokens.text, fontSize: 28, fontWeight: "600" },
  muted: { color: tokens.muted, fontSize: 14 },
  dim: { color: tokens.dim, fontSize: 12 },
  gold: { color: tokens.gold, fontSize: 13 },
  kicker: { color: tokens.gold, fontSize: 11, fontWeight: "600" },
  gap: { gap: 8 },
});
