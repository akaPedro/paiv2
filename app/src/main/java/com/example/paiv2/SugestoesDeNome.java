package com.example.paiv2;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Escolhe, entre os textos lidos numa embalagem, os que servem para compor o
 * nome do produto. Não depende do Android para poder ser testada isoladamente.
 */
public final class SugestoesDeNome {

    /** Uma linha de texto lida na foto e a altura dela, em pixels. */
    public static final class Linha {
        final String texto;
        final int altura;

        public Linha(String texto, int altura) {
            this.texto = texto;
            this.altura = altura;
        }
    }

    private static final int MAX_TEXTOS = 6;
    private static final int MAX_MEDIDAS = 2;
    private static final int MAX_CARACTERES = 32;
    private static final int MAX_PALAVRAS = 5;
    // Linha menor que isto, em relação ao maior texto da foto, é letra miúda de rótulo.
    // Calibrado com leituras reais: texto útil ficou entre 29% e 45%; lixo, até 20%.
    private static final int PERCENTUAL_ALTURA_MINIMA = 25;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private static final Pattern MEDIDA = Pattern.compile(
            "(?<![\\d.,])(\\d{1,4}(?:[.,]\\d{1,3})?)\\s?(kg|g|mg|ml|l|lt|litros?|un|unid)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DIGITOS_DEMAIS = Pattern.compile("\\d{4,}");

    // Trechos típicos de rótulo que nunca fazem parte do nome
    private static final String[] PROIBIDOS = {
            "aviso", "importante", "ingrediente", "contem", "conteudo", "validade", "lote",
            "fabricad", "industria", "www", "http", ".com", "sac ", "cnpj", "tabela",
            "nutricional", "energetico", "porcao", "gluten", "lactose", "alergic", "informac",
            "peso liquido", "peso liq", "peso lig", "peso bruto", "conservar", "consumir",
            "modo de preparo", "crianca", "medico", "nutricionista", "aleitamento"
    };

    // Ficam em minúsculas no meio do nome: "Leite em Pó", não "Leite Em Pó"
    private static final Set<String> PALAVRAS_MINUSCULAS = new HashSet<>(Arrays.asList(
            "de", "da", "do", "das", "dos", "e", "em", "com", "para", "sem", "a", "o"));

    private SugestoesDeNome() {
    }

    public static List<String> escolher(List<Linha> linhas) {
        List<String> medidas = new ArrayList<>();
        Set<String> medidasVistas = new HashSet<>();
        List<Linha> candidatas = new ArrayList<>();

        for (Linha linha : linhas) {
            if (linha.texto == null) continue;
            String limpo = limpar(linha.texto);
            if (limpo.isEmpty()) continue;

            // Medidas são aproveitadas mesmo de linhas longas: "Peso líquido 200g"
            Matcher m = MEDIDA.matcher(limpo);
            while (m.find()) {
                String medida = formatarMedida(m.group(1), m.group(2));
                if (medidasVistas.add(medida.toLowerCase(Locale.ROOT))) medidas.add(medida);
            }

            if (serveComoNome(limpo)) candidatas.add(new Linha(limpo, linha.altura));
        }

        // Letra maior na embalagem costuma ser a marca e o nome do produto.
        // A ordenação é estável: em empate, fica a ordem de leitura.
        candidatas.sort((a, b) -> Integer.compare(b.altura, a.altura));

        // Letra miúda é onde a leitura mais erra ("Indústria Brasileira" vira
        // "indisie Boeslels") e nunca é o nome do produto
        int maiorAltura = candidatas.isEmpty() ? 0 : candidatas.get(0).altura;
        candidatas.removeIf(c -> c.altura * 100 < maiorAltura * PERCENTUAL_ALTURA_MINIMA);

        List<String> resultado = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Linha candidata : candidatas) {
            if (resultado.size() >= MAX_TEXTOS) break;
            String formatado = ajustarCaixa(candidata.texto);
            if (vistos.add(chave(formatado))) resultado.add(formatado);
        }
        for (int i = 0; i < medidas.size() && i < MAX_MEDIDAS; i++) {
            resultado.add(medidas.get(i));
        }
        return resultado;
    }

    static String limpar(String texto) {
        String s = texto.replaceAll("[®™©•*|_~\"“”]", " ").replaceAll("\\s+", " ").trim();
        return s.replaceAll("^[\\p{Punct}\\s]+", "").replaceAll("[\\p{Punct}\\s]+$", "");
    }

    static boolean serveComoNome(String s) {
        if (s.length() < 2 || s.length() > MAX_CARACTERES) return false;
        if (s.split(" ").length > MAX_PALAVRAS) return false;

        int letras = contarLetras(s);
        // Fragmentos como "na", lidos de logotipos estilizados, também distorceriam
        // a régua de altura, já que costumam ser os maiores da foto
        if (letras < 3) return false;
        // Mais números e sinais que letras: código de barras, data, lote
        if (letras * 2 < s.replace(" ", "").length()) return false;
        // Número de 4 dígitos ou mais: código, lote ou medida lida errado ("497g" virando "4979")
        if (DIGITOS_DEMAIS.matcher(s).find()) return false;
        // Linha que é só a medida já entra na lista de medidas
        if (MEDIDA.matcher(s).matches()) return false;

        String normalizado = chave(s) + " ";
        for (String proibido : PROIBIDOS) {
            if (normalizado.contains(proibido)) return false;
        }
        return true;
    }

    /** Linha toda em maiúsculas vira "Título": "LEITE EM PÓ" → "Leite em Pó". */
    static String ajustarCaixa(String s) {
        if (!todaMaiuscula(s)) return s;

        String[] palavras = s.toLowerCase(PT_BR).split(" ");
        StringBuilder saida = new StringBuilder();
        for (int i = 0; i < palavras.length; i++) {
            String palavra = palavras[i];
            if (saida.length() > 0) saida.append(' ');
            if (palavra.isEmpty() || (i > 0 && PALAVRAS_MINUSCULAS.contains(palavra))) {
                saida.append(palavra);
            } else {
                saida.append(Character.toUpperCase(palavra.charAt(0))).append(palavra.substring(1));
            }
        }
        return saida.toString();
    }

    static String formatarMedida(String numero, String unidade) {
        String u = unidade.toLowerCase(Locale.ROOT);
        if (u.equals("l") || u.equals("lt") || u.startsWith("litro")) u = "L";
        else if (u.equals("unid")) u = "un";
        return numero + " " + u;
    }

    private static boolean todaMaiuscula(String s) {
        boolean temLetra = false;
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) {
                temLetra = true;
                if (!Character.isUpperCase(c)) return false;
            }
        }
        return temLetra;
    }

    private static int contarLetras(String s) {
        int letras = 0;
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) letras++;
        }
        return letras;
    }

    private static String chave(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
