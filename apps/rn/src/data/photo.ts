import * as ImageManipulator from "expo-image-manipulator";
import * as ImagePicker from "expo-image-picker";

import { QUALIDADE_JPEG, dimensoesFoto } from "../domain/fotoEscala";

/** Picker → lado maior ≤1280 → JPEG 70 → base64. O caller não grava a foto. */
export async function escolherFotoB64(): Promise<string | null> {
  const picked = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ["images"],
    quality: 1,
  });
  if (picked.canceled || !picked.assets[0]) return null;
  const asset = picked.assets[0];
  const alvo = dimensoesFoto(asset.width ?? 0, asset.height ?? 0);
  const resultado = await ImageManipulator.manipulateAsync(
    asset.uri,
    [{ resize: { width: alvo.largura, height: alvo.altura } }],
    { compress: QUALIDADE_JPEG / 100, format: ImageManipulator.SaveFormat.JPEG, base64: true },
  );
  return resultado.base64 ?? null;
}
