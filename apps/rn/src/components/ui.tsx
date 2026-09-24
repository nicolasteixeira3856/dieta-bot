import type { ReactNode } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { Button, TextInput } from "react-native-paper";

import { tokens } from "../theme/tokens";

export function Cta({
  label,
  onPress,
  disabled,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
}) {
  return (
    <Button
      mode="contained"
      buttonColor={tokens.cta}
      textColor={tokens.ctaText}
      onPress={onPress}
      disabled={disabled}
      accessibilityLabel={label}
      style={estilos.cta}
      labelStyle={estilos.ctaLabel}
    >
      {label}
    </Button>
  );
}

export function CampoNumero({
  label,
  valor,
  suffix = "kcal",
  onChange,
}: {
  label: string;
  valor: string;
  suffix?: string;
  onChange: (valor: string) => void;
}) {
  return (
    <TextInput
      mode="outlined"
      label={label}
      value={valor}
      onChangeText={(texto) => onChange(texto.replace(/[^0-9]/g, ""))}
      keyboardType="number-pad"
      textColor={tokens.text}
      outlineColor={tokens.line}
      activeOutlineColor={tokens.gold}
      style={estilos.campo}
      contentStyle={estilos.campoTexto}
      right={<TextInput.Affix text={suffix} />}
      accessibilityLabel={label}
    />
  );
}

export function Escolha({
  titulo,
  corpo,
  ativo,
  onPress,
}: {
  titulo: string;
  corpo?: string;
  ativo: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityLabel={titulo}
      style={[estilos.escolha, ativo && estilos.escolhaAtiva]}
    >
      <Text style={[estilos.escolhaTitulo, ativo && estilos.ativo]}>{titulo}</Text>
      {corpo ? <Text style={estilos.corpo}>{corpo}</Text> : null}
    </Pressable>
  );
}

export function Cartao({ children }: { children: ReactNode }) {
  return <View style={estilos.cartao}>{children}</View>;
}

const estilos = StyleSheet.create({
  cta: { borderRadius: 14, marginTop: 4 },
  ctaLabel: { fontSize: 16, fontWeight: "600" },
  campo: { backgroundColor: tokens.surf, fontSize: 28 },
  campoTexto: { fontSize: 28, color: tokens.text },
  escolha: {
    borderWidth: 1,
    borderColor: tokens.line,
    borderRadius: 14,
    backgroundColor: tokens.surf,
    padding: 14,
    gap: 4,
  },
  escolhaAtiva: { borderColor: tokens.gold },
  escolhaTitulo: { color: tokens.text, fontSize: 16 },
  ativo: { color: tokens.gold },
  corpo: { color: tokens.muted, fontSize: 13 },
  cartao: {
    backgroundColor: tokens.surf,
    borderColor: tokens.line,
    borderWidth: 1,
    borderRadius: 14,
    padding: 16,
    gap: 6,
  },
});
