FROM python:2-onbuild

EXPOSE 5000

# Create the earth engine credentials in the docker environment
ENV HOME /usr/src/app
RUN mkdir -p /usr/src/app/.config/earthengine
COPY earthengine_token.json /usr/src/app/.config/earthengine/credentials

CMD [ "python", "imagefetcher/__main__.py" ]
