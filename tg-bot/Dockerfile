FROM amazoncorretto:21-alpine

# Установка дополнительных пакетов, если требуется
RUN apk add --no-cache ttf-dejavu

EXPOSE 8443

WORKDIR /PlanAceBot/tg-bot

COPY ./fonts/FreeSans.ttf ./fonts/FreeSans.ttf

RUN fc-cache -f -v

COPY ./target/*.jar .

CMD java -jar *.jar
