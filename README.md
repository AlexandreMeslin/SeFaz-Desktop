# Example-MUSANet-Desktop

Para executar:

1) Lembre-se de colocar o Gateway no ar:

docker-compose -f docker-start-gw.yml up

2) Execute o GroupDefiner

java -jar MainGD.jar

3) Execute o Processing Node (para trocar mensagens)

java -jar MainPN.jar

4) Execute o Processing Node (para monitorar fiscais) 

java -jar MainPNCEP.jar
