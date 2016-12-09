import { Query } from './query';

/** Mocks queries array that would retrieve at route /monitoring/queries */
export let QUERIES: Query[] = [
  {
    "id": "1",
    "name": "Tous les incendies entre 2003 et 2009",
    "author": "asedhafe654678ehh3526",
    "status": "running",
    "tasks": [
      {
        "id": "1",
        "type": "Image downloading",
        "progress": 5,
        "metadata": {
          "images": [
            {
              "name": "example01.png"
            }, {
              "name": "example02.png"
            }
          ]
        }
      }, {
        "id": "2",
        "type": "Superposition",
        "progress": 15,
        "metadata": {}
      }
    ]
  },
  {
    "id": "2",
    "name": "Déforestation en Amazonie en 2011",
    "author": "wedsdfe654678ehhe3526",
    "status": "running",
    "tasks": [
      {
        "id": "3",
        "type": "Image downloading",
        "progress": 100,
        "metadata": {
          "images": [
            {
              "name": "example09.png"
            }
          ]
        }
      }, {
        "id": "4",
        "type": "Region partitionning",
        "progress": 50,
        "metadata": {}
      }
    ]
  },
  {
    "id": "3",
    "name": "Toutes les innnondations en France en 2010",
    "author": "asedhafe654678ehh3526",
    "status": "running",
    "tasks": [
      {
        "id": "5",
        "type": "Image downloading",
        "progress": 2,
        "metadata": {
          "images": [
            {
              "name": "example19.png"
            }
          ]
        }
      }
    ]
  }
];
