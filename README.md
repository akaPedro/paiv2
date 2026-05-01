PAIV2 - Gerenciador de Produtos e Estoque
(projeto desenvolvido a meu pai)

O PAIV2 é um aplicativo Android desenvolvido para a gestão eficiente de catálogos de produtos. Ele se destaca pela capacidade de processar grandes volumes de imagens e organizar itens de forma inteligente, garantindo performance e estabilidade mesmo em dispositivos com recursos limitados.
  
  Principais Funcionalidades

    Organização por Categorias: Divisão lógica de itens, como a separação automática entre Bebidas Alcoólicas e Não Alcoólicas usando GridLayoutManager.

    Importação em Lote (Bulk Import): Seleção múltipla de imagens da galeria com geração automática de nomes e indexação.

    Processamento Inteligente de Imagens:

        Redimensionamento automático para 800px de largura para economizar memória RAM.

        Compressão JPEG a 80-85% de qualidade para otimizar o armazenamento.

        Uso de bitmap.recycle() para evitar vazamentos de memória (OutOfMemoryError) durante importações massivas.

    Armazenamento Interno Seguro: Cópia física de arquivos para o diretório privado do App (files/), resolvendo o erro clássico de SecurityException (perda de permissão de URI da galeria).

    Visualização Avançada: Grid de 3 colunas com cabeçalhos dinâmicos (SpanSizeLookup) e visualização de imagem em tela cheia via Glide.

Stack Tecnológica

    Linguagem: Java

    Banco de Dados: Room Database (SQLite) para persistência de dados local.

    Carregamento de Imagens: Glide (suporte a Assets, File System e Content Providers).

    Interface: Material Design, ConstraintLayout e RecyclerView com múltiplos ViewTypes.

Estrutura do Projeto

    AddProdutoActivity: Gerencia a captura de imagens e salvamento em lote no banco de dados.

    BebActivity: Tela principal de bebidas com lógica de separação alcoólica/não alcoólica.

    ImagemProdutoActivity: Visualizador de alta resolução para detalhes do produto.

    ProdutoAdapter: O coração da UI, gerenciando a exibição de cards e divisores.

    database/: Configurações do Room e DAOs para acesso aos dados.

Desenvolvido com foco em performance e facilidade de uso.
