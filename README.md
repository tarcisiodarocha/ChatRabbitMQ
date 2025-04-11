# Descrição do Projeto

Este projeto consiste no desenvolvimento de um cliente de chat do tipo linha de comando usando o RabbitMQ como servidor de mensagens de acordo com o apresentado em sala de aula.

Este projeto será desenvolvido composto por cinco etapas, conforme o andamento das aulas da disciplina. Cada etapa será descrita em um arquivo ".md" específico neste mesmo repositório.

Inicialmente, esse projeto usará uma instância do RabbitMQ instalada no serviço EC2 da AWS. Os passos para essa instalação se encontra no arquivo "rabbitmw-ec2.md" neste mesmo repositório.

### Criação do Repositório para o Projeto

Esse projeto deve ser feito a partir do seguinte repositório base a ser criado automaticamento pelo GitHub Classroom clicando no link a seguir: 

https://classroom.github.com/a/7G_WRqkb

Somente um integrante do grupo precisa criar o repositório a partir do link acima. Os demais integrantes do grupo devem ser inseridos posteriormente como colaboradores do repositório para que todos os membros do grupo tenham acesso.

### Instruções para criar e submeter cada etapa em branches diferentes

Cada etapa deve ser criada e submetida em um branch diferente do GitHub. Para isso, siga as instruções abaixo:

1. **Clonar o repositório**
   - Primeiro, clonar o repositório criado para o seu computador (ou Cloud9). No terminal, execute:
     ```bash
     git clone <URL_DO_REPOSITÓRIO>
     ```
   - Substitua `<URL_DO_REPOSITÓRIO>` pela URL do repositório fornecida.

2. **Criar um novo branch para a primeira etapa**
   - Navegue até o diretório do repositório clonado:
     ```bash
     cd <NOME_DO_REPOSITÓRIO>
     ```
   - Crie um novo branch. Por exemplo, para a etapa 1:
     ```bash
     git checkout -b etapa-1
     ```

3. **Fazer alterações e commit**
   - Faça as alterações necessárias para implementar a etapa do projeto.
   - Adicione os arquivos modificados ao staging:
     ```bash
     git add .
     ```
   - Faça o commit das alterações:
     ```bash
     git commit -m "Finalizando a etapa 1 do projeto"
     ```

4. **Enviar o branch para o GitHub (submissão da etapa para o professor)**
   - Envie o branch criado para o repositório remoto:
     ```bash
     git push origin etapa-1
     ```

5. **Criar um novo branch para a próxima etapa**
   - Para cada nova etapa, crie um novo branch a partir do branch anterior. Por exemplo, para a etapa 2:
     ```bash
     git checkout -b etapa-2 etapa-1
     ```

### Trabalhar em um branch diferente

1. **Listar branches existentes**
   - Para ver todos os branches disponíveis no repositório, execute:
     ```bash
     git branch
     ```

2. **Alternar para um branch diferente**
   - Para alternar para um branch existente, use o comando `checkout`. Por exemplo, para alternar para o branch `etapa-2`:
     ```bash
     git checkout etapa-2
     ```

3. **Atualizar o branch local**
   - Para garantir que você tenha as últimas alterações do branch remoto, execute:
     ```bash
     git pull origin etapa-2
     ```

