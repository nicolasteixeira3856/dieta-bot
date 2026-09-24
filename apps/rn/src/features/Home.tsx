import { useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useMutation, useQuery } from "@tanstack/react-query";
import { TextInput } from "react-native-paper";

import { Cta } from "../components/ui";
import { encaixarRefeicao, estimarRefeicao, health, type Encaixe } from "../data/api";
import { orcamentoDoDia, useDia } from "../data/day";
import { escolherFotoB64 } from "../data/photo";
import type { Estimativa } from "../domain/rede";
import { JANELAS, tituloJanela } from "../domain/janela";
import { Folhas, type Folha } from "./Sheets";
import { tokens } from "../theme/tokens";

export function Home() {
  const { dia, patch, confirmarLog, removerChip, responderChip } = useDia();
  const insets = useSafeAreaInsets();
  const orcamento = orcamentoDoDia(dia);
  const [folha, setFolha] = useState<Folha | null>(null);
  const [texto, setTexto] = useState("");
  const [foto, setFoto] = useState<string | null>(null);
  const [estimativa, setEstimativa] = useState<Estimativa | null>(null);
  const [modo, setModo] = useState<"quero" | "tenho" | "ideia">("quero");
  const [encaixeTexto, setEncaixeTexto] = useState("");
  const [encaixe, setEncaixe] = useState<Encaixe | null>(null);

  useQuery({ queryKey: ["health"], queryFn: health, retry: false });

  const registrar = useMutation({
    mutationFn: () => estimarRefeicao({ janela: orcamento.janela, text: texto, image_b64: foto }),
    onSuccess: (out) => {
      setEstimativa(out);
      setFoto(null);
      setFolha("t2");
    },
  });

  const fit = useMutation({
    mutationFn: () => {
      const mode = modo === "quero" ? "want" : modo === "tenho" ? "have" : "surprise";
      const itens = modo === "tenho" ? encaixeTexto.split(",").map((item) => item.trim()).filter(Boolean) : [];
      return encaixarRefeicao({
        mode,
        text: modo === "ideia" ? "" : encaixeTexto,
        itens,
        orcamentoKcal: orcamento.orcamentoJanela,
        orcamentoP: Math.max(0, 170 - orcamento.proteina),
      });
    },
    onSuccess: (out) => setEncaixe(out),
  });

  const frac =
    orcamento.tetoEfetivo === 0 ? 0 : Math.min(1, Math.max(0.02, orcamento.comido / orcamento.tetoEfetivo));
  const dataCurta = `${String(orcamento.data.d).padStart(2, "0")}/${String(orcamento.data.m).padStart(2, "0")}`;

  return (
    <View style={estilos.tela}>
      <ScrollView contentContainerStyle={[estilos.conteudo, { paddingTop: insets.top + 12 }]}>
        <View style={estilos.linha}>
          <Text style={estilos.muted}>{dataCurta}</Text>
          <Text style={estilos.teto}>Teto {orcamento.tetoEfetivo}</Text>
        </View>
        <Text style={estilos.saldo} accessibilityLabel="saldo">
          {Math.round(orcamento.comido)}  / {Math.round(orcamento.tetoEfetivo)} kcal
        </Text>
        <Text style={estilos.muted}>P {Math.round(orcamento.proteina)} / 170 g</Text>
        <View style={estilos.trilho}>
          <View style={[estilos.barra, { width: `${frac * 100}%` }]} />
        </View>
        <View style={estilos.cartao}>
          <Text style={estilos.kicker}>PRÓXIMA</Text>
          <Text style={estilos.janela}>{tituloJanela(orcamento.janela)}</Text>
          <Text style={estilos.orcamento}>Orçamento {Math.round(orcamento.orcamentoJanela)} kcal</Text>
          <Text style={estilos.muted}>Ainda sem atalho. Escreve ou manda foto.</Text>
          <Cta
            label="O que cabe agora"
            onPress={() => {
              setEncaixe(null);
              setFolha("t3");
            }}
          />
        </View>
        <Pressable
          style={estilos.composer}
          onPress={() => setFolha("t1")}
          accessibilityLabel="O que você comeu, ou uma foto"
        >
          <Text style={estilos.muted}>O que você comeu, ou uma foto</Text>
          <Text style={estilos.gold}>◉</Text>
        </Pressable>
        {dia.diaApp > 1 && orcamento.chip ? (
          <View style={estilos.cartao}>
            <Text style={estilos.gold}>{tituloJanela(orcamento.chip.janela)}</Text>
            {orcamento.chip.pergunta ? <Text style={estilos.texto}>Quer um atalho desta janela?</Text> : null}
            {orcamento.chip.pergunta ? (
              <View style={estilos.linha}>
                <Text style={estilos.gold} onPress={() => responderChip(orcamento.chip!.janela, true)}>
                  Criar
                </Text>
                <Text style={estilos.muted} onPress={() => responderChip(orcamento.chip!.janela, false)}>
                  Não
                </Text>
              </View>
            ) : null}
            <Text style={estilos.bad} onPress={() => removerChip(orcamento.chip!.janela)} accessibilityLabel="Remover">
              Remover
            </Text>
          </View>
        ) : null}
        {JANELAS.map((id) => (
          <View key={id} style={estilos.linha}>
            <Text style={id === orcamento.janela ? estilos.texto : estilos.muted}>{tituloJanela(id)}</Text>
            <Text style={id === orcamento.janela ? estilos.gold : estilos.dim}>
              {id === orcamento.janela ? "agora" : "pendente"}
            </Text>
          </View>
        ))}
        <Text style={estilos.muted}>{dia.treino.trim() ? `Treino ${dia.treino} kcal` : "Treino hoje"}</Text>
        <TextInput
          mode="outlined"
          label="treino kcal"
          value={dia.treino}
          onChangeText={(treino) => patch({ treino: treino.replace(/[^0-9]/g, "") })}
          keyboardType="number-pad"
          textColor={tokens.text}
          outlineColor={tokens.line}
          activeOutlineColor={tokens.gold}
          style={estilos.treino}
          accessibilityLabel="treino kcal"
        />
        <Text style={estilos.dim}>estimativa, não consulta</Text>
      </ScrollView>
      <Folhas
        folha={folha}
        onFechar={() => setFolha(null)}
        janela={orcamento.janela}
        orcamento={orcamento.orcamentoJanela}
        tetoEfetivo={orcamento.tetoEfetivo}
        comido={orcamento.comido}
        texto={texto}
        onTexto={setTexto}
        fotoPronta={foto != null}
        onFoto={() => {
          void escolherFotoB64().then((b64) => setFoto(b64));
        }}
        carregando={registrar.isPending || fit.isPending}
        onRegistrar={() => registrar.mutate()}
        estimativa={estimativa}
        onConfirmar={() => {
          if (!estimativa) return;
          confirmarLog({
            janela: orcamento.janela,
            kcal: estimativa.kcal,
            p: estimativa.p,
            c: estimativa.c,
            g: estimativa.g,
            estavel: true,
          });
          setTexto("");
          setFolha(null);
        }}
        modo={modo}
        onModo={setModo}
        encaixeTexto={encaixeTexto}
        onEncaixeTexto={setEncaixeTexto}
        onEncaixar={() => fit.mutate()}
        encaixe={encaixe}
      />
    </View>
  );
}

const estilos = StyleSheet.create({
  tela: { flex: 1, backgroundColor: tokens.bg },
  conteudo: { padding: 20, gap: 12, paddingBottom: 48 },
  linha: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  muted: { color: tokens.muted, fontSize: 13 },
  teto: { color: tokens.text, fontSize: 14, fontWeight: "600" },
  saldo: { color: tokens.text, fontSize: 34, fontWeight: "600" },
  trilho: { height: 6, borderRadius: 99, backgroundColor: tokens.line, overflow: "hidden" },
  barra: { height: 6, backgroundColor: tokens.gold },
  cartao: {
    backgroundColor: tokens.surf,
    borderColor: tokens.line,
    borderWidth: 1,
    borderRadius: 14,
    padding: 16,
    gap: 6,
  },
  kicker: { color: tokens.gold, fontSize: 11, fontWeight: "600" },
  janela: { color: tokens.text, fontSize: 20, fontWeight: "600" },
  orcamento: { color: tokens.text, fontSize: 16 },
  composer: {
    borderWidth: 1,
    borderColor: tokens.line,
    borderRadius: 14,
    backgroundColor: tokens.surf,
    padding: 14,
    flexDirection: "row",
    justifyContent: "space-between",
  },
  texto: { color: tokens.text, fontSize: 15 },
  gold: { color: tokens.gold },
  dim: { color: tokens.dim, fontSize: 12 },
  bad: { color: tokens.bad },
  treino: { backgroundColor: tokens.surf },
});
