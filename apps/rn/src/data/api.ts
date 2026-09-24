import { z } from "zod";

import { localTimeSaoPaulo } from "../domain/budgetCalculator";
import { ofertasQueCabem, type PratoOferta } from "../domain/fitGuard";
import { estimarComFalha, PERGUNTA_FALHA, type Estimativa } from "../domain/rede";

const URL_EXEMPLO = "http://127.0.0.1:8080";
const CONVITE_EXEMPLO = "troca-isto";

export function apiBase(): string {
  return (process.env.EXPO_PUBLIC_API_PUBLIC_URL || URL_EXEMPLO).replace(/\/$/, "");
}

export function inviteCode(): string {
  return process.env.EXPO_PUBLIC_INVITE_CODE || CONVITE_EXEMPLO;
}

const healthSchema = z.object({
  ok: z.boolean(),
  model: z.string(),
});

const estimateSchema = z.object({
  kcal: z.number(),
  p: z.number(),
  c: z.number().optional().default(0),
  g: z.number().optional().default(0),
  confianca: z.string(),
  pergunta: z.string().nullable().optional(),
});

const porcaoSchema = z.object({
  nome: z.string().default(""),
  quantidade: z.string().default(""),
});

const pratoSchema = z.object({
  nome: z.string().default(""),
  kcal: z.number().default(0),
  p: z.number().default(0),
  porcoes: z.array(porcaoSchema).default([]),
});

const fitSchema = z.object({
  prato: pratoSchema.optional(),
  cabe: z.boolean().optional(),
  pergunta: z.string().optional().default(""),
  opcoes: z.array(pratoSchema).optional().default([]),
});

export type Prato = z.infer<typeof pratoSchema>;

export type Encaixe = {
  pratos: Prato[];
  cabe: boolean;
  pergunta: string;
};

async function chamar(path: string, body?: unknown): Promise<unknown> {
  const timer = AbortSignal.timeout(20_000);
  const resposta = await fetch(`${apiBase()}${path}`, {
    method: body === undefined ? "GET" : "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      "X-Invite": inviteCode(),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: timer,
  });
  if (!resposta.ok) throw new Error(String(resposta.status));
  return resposta.json();
}

export async function health(): Promise<{ ok: boolean; model: string }> {
  return healthSchema.parse(await chamar("/health"));
}

export async function estimarRefeicao(entrada: {
  janela: string;
  text: string;
  image_b64: string | null;
}): Promise<Estimativa> {
  return estimarComFalha(async () => {
    const json = await chamar("/v1/estimate", {
      local_time: localTimeSaoPaulo(),
      janela: entrada.janela,
      text: entrada.text,
      image_b64: entrada.image_b64,
    });
    const parsed = estimateSchema.parse(json);
    return {
      kcal: parsed.kcal,
      p: parsed.p,
      c: parsed.c,
      g: parsed.g,
      confianca: parsed.confianca,
      pergunta: parsed.pergunta,
    };
  });
}

export async function encaixarRefeicao(entrada: {
  mode: "want" | "have" | "surprise";
  text: string;
  itens: string[];
  orcamentoKcal: number;
  orcamentoP: number;
}): Promise<Encaixe> {
  try {
    const json = await chamar("/v1/fit", {
      mode: entrada.mode,
      text: entrada.text,
      itens_disponiveis: entrada.itens,
      orcamento: { kcal: entrada.orcamentoKcal, p: entrada.orcamentoP },
      image_b64: null,
    });
    const parsed = fitSchema.parse(json);
    const brutos: Prato[] = [...(parsed.opcoes ?? [])];
    if (parsed.prato) brutos.push(parsed.prato);
    const cabem = new Set(
      ofertasQueCabem(
        brutos.map((prato) => ({ nome: prato.nome, kcal: prato.kcal })),
        entrada.orcamentoKcal,
      ).map((prato: PratoOferta) => prato.nome),
    );
    const pratos = brutos.filter((prato) => cabem.has(prato.nome));
    return {
      pratos,
      cabe: pratos.length > 0,
      pergunta: parsed.pergunta.trim() ? parsed.pergunta : PERGUNTA_FALHA,
    };
  } catch {
    return { pratos: [], cabe: false, pergunta: PERGUNTA_FALHA };
  }
}
