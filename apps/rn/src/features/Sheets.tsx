import { useRef } from "react";
import { StyleSheet, Text, View } from "react-native";
import BottomSheet, { BottomSheetView } from "@gorhom/bottom-sheet";
import { TextInput } from "react-native-paper";

import { Cta, Escolha } from "../components/ui";
import type { Encaixe } from "../data/api";
import type { Estimativa } from "../domain/rede";
import { tituloJanela } from "../domain/janela";
import { tokens } from "../theme/tokens";

export type Folha = "t1" | "t2" | "t3";

type Props = {
  folha: Folha | null;
  onFechar: () => void;
  janela: string;
  orcamento: number;
  tetoEfetivo: number;
  comido: number;
  texto: string;
  onTexto: (valor: string) => void;
  fotoPronta: boolean;
  onFoto: () => void;
  carregando: boolean;
  onRegistrar: () => void;
  estimativa: Estimativa | null;
  onConfirmar: () => void;
  modo: "quero" | "tenho" | "ideia";
  onModo: (modo: "quero" | "tenho" | "ideia") => void;
  encaixeTexto: string;
  onEncaixeTexto: (valor: string) => void;
  onEncaixar: () => void;
  encaixe: Encaixe | null;
};

export function Folhas(props: Props) {
  const jaAbriu = useRef(false);
  if (!props.folha) return null;

  return (
    <BottomSheet
      index={0}
      snapPoints={["72%"]}
      enableDynamicSizing={false}
      enablePanDownToClose
      onChange={(indice) => {
        if (indice >= 0) jaAbriu.current = true;
        else if (jaAbriu.current) {
          jaAbriu.current = false;
          props.onFechar();
        }
      }}
      backgroundStyle={estilos.fundo}
      handleIndicatorStyle={estilos.alca}
    >
      <BottomSheetView style={estilos.miolo}>
        {props.folha === "t1" ? <Registro {...props} /> : null}
        {props.folha === "t2" ? <Confirma {...props} /> : null}
        {props.folha === "t3" ? <Encaixe {...props} /> : null}
      </BottomSheetView>
    </BottomSheet>
  );
}

function Registro(props: Props) {
  return (
    <View style={estilos.col} accessibilityLabel="registrar">
      <Text style={estilos.kicker}>REGISTRAR</Text>
      <TextInput
        mode="outlined"
        value={props.texto}
        onChangeText={props.onTexto}
        placeholder="2 paes, ovo, cafe com leite"
        placeholderTextColor={tokens.dim}
        textColor={tokens.text}
        outlineColor={tokens.line}
        activeOutlineColor={tokens.gold}
        style={estilos.campo}
        accessibilityLabel="t1-texto"
      />
      <Text style={estilos.link} onPress={props.onFoto} accessibilityLabel="Foto">
        {props.fotoPronta ? "Foto pronta" : "Foto"}
      </Text>
      <Cta label={props.carregando ? "…" : "Registrar"} onPress={props.onRegistrar} disabled={props.carregando} />
    </View>
  );
}

function Confirma(props: Props) {
  const est = props.estimativa;
  const kcal = Math.round(est?.kcal ?? 0);
  const semResposta = est == null || (est.confianca === "baixa" && kcal === 0);
  const cabe = !semResposta && kcal <= props.orcamento;
  const saldo = Math.max(0, props.tetoEfetivo - props.comido - kcal);
  return (
    <View style={estilos.col} accessibilityLabel="t2-card">
      <Text style={estilos.kicker}>{tituloJanela(props.janela)}</Text>
      <Text style={estilos.kcal} accessibilityLabel={`${kcal} kcal`}>
        ≈ {kcal} kcal
      </Text>
      <Text style={estilos.muted}>
        P {Math.round(est?.p ?? 0)} · C {Math.round(est?.c ?? 0)} · G {Math.round(est?.g ?? 0)}
      </Text>
      <Text style={estilos.gold}>confiança {est?.confianca ?? "baixa"}</Text>
      <Text style={{ color: cabe ? tokens.good : tokens.bad, fontSize: 18 }} accessibilityLabel={semResposta ? "sem resposta" : cabe ? "cabe" : "não cabe"}>
        {semResposta ? "sem resposta" : cabe ? "cabe" : "não cabe"}
      </Text>
      {est?.confianca !== "alto" ? (
        <Text style={estilos.texto}>{est?.pergunta || "descreve em 1 linha"}</Text>
      ) : null}
      <Text style={estilos.muted}>
        Saldo {saldo} / {props.tetoEfetivo}
      </Text>
      <Text style={estilos.dim}>estimativa, não consulta</Text>
      <Cta label="Confirmar" onPress={props.onConfirmar} />
    </View>
  );
}

function Encaixe(props: Props) {
  return (
    <View style={estilos.col} accessibilityLabel="encaixe">
      <Text style={estilos.kicker}>O QUE CABE AGORA</Text>
      <Text style={estilos.texto}>
        {tituloJanela(props.janela)} · teto {props.orcamento} kcal
      </Text>
      <View style={estilos.modos}>
        <Escolha titulo="Quero" ativo={props.modo === "quero"} onPress={() => props.onModo("quero")} />
        <Escolha titulo="Tenho" ativo={props.modo === "tenho"} onPress={() => props.onModo("tenho")} />
        <Escolha titulo="Sem ideia" ativo={props.modo === "ideia"} onPress={() => props.onModo("ideia")} />
      </View>
      {props.modo !== "ideia" ? (
        <TextInput
          mode="outlined"
          value={props.encaixeTexto}
          onChangeText={props.onEncaixeTexto}
          placeholder={props.modo === "quero" ? "quero hambúrguer caseiro" : "pão, frango, queijo"}
          placeholderTextColor={tokens.dim}
          textColor={tokens.text}
          outlineColor={tokens.line}
          activeOutlineColor={tokens.gold}
          style={estilos.campo}
          accessibilityLabel="t3-texto"
        />
      ) : null}
      <Cta label={props.carregando ? "…" : "Encaixar"} onPress={props.onEncaixar} disabled={props.carregando} />
      {props.encaixe ? (
        props.encaixe.pratos.length === 0 ? (
          <Text style={{ color: tokens.bad }}>não cabe</Text>
        ) : (
          props.encaixe.pratos.map((prato) => (
            <View key={`${prato.nome}-${prato.kcal}`} style={estilos.prato}>
              <Text style={estilos.texto}>{prato.nome}</Text>
              <Text style={{ color: tokens.good }}>
                {Math.round(prato.kcal)} kcal · P {Math.round(prato.p)} · cabe
              </Text>
              {prato.porcoes.map((porcao) => (
                <Text key={`${porcao.quantidade}-${porcao.nome}`} style={estilos.muted}>
                  {porcao.quantidade} {porcao.nome}
                </Text>
              ))}
            </View>
          ))
        )
      ) : null}
      {props.encaixe?.pergunta ? <Text style={estilos.texto}>{props.encaixe.pergunta}</Text> : null}
      <Text style={estilos.dim}>estimativa, não consulta</Text>
    </View>
  );
}

const estilos = StyleSheet.create({
  fundo: {
    backgroundColor: tokens.surf,
    borderTopLeftRadius: 22,
    borderTopRightRadius: 22,
  },
  alca: { backgroundColor: tokens.handle, width: 36 },
  miolo: { padding: 20, gap: 12 },
  col: { gap: 12 },
  kicker: { color: tokens.gold, fontSize: 11, fontWeight: "600" },
  campo: { backgroundColor: tokens.surf },
  link: { color: tokens.gold, fontSize: 16 },
  kcal: { color: tokens.text, fontSize: 32, fontWeight: "600" },
  muted: { color: tokens.muted, fontSize: 14 },
  gold: { color: tokens.gold },
  texto: { color: tokens.text, fontSize: 16 },
  dim: { color: tokens.dim, fontSize: 12 },
  modos: { flexDirection: "row", gap: 8 },
  prato: {
    borderWidth: 1,
    borderColor: tokens.line,
    borderRadius: 14,
    padding: 12,
    gap: 4,
  },
});
