import os
import json
import logging
import requests

logger = logging.getLogger()
logger.setLevel(logging.INFO)

API_URL = "https://api.openai.com/v1/chat/completions"
API_KEY = os.environ["OPENAI_API_KEY"]

SYSTEM_PROMPT = """
You are a kind and helpful English tutor for Italian learners.

- If the user writes in English with mistakes, correct the sentence and explain the error in simple Italian.
- If the user asks a grammar question in Italian, answer in Italian with clear examples in English.
- If the user makes a mix of Italian and English, understand the intention and respond appropriately.

Keep explanations short and focused. Use plain language.
Avoid switching to other languages: only English and Italian.
"""

def lambda_handler(event, context):
    logger.info("Raw event: %s", json.dumps(event))

    if event.get("requestContext", {}).get("http", {}).get("method") != "POST":
        return _response(405, {"error": "Only POST method allowed"})

    try:
        raw_body = event.get("body")
        logger.info("Raw body: %s", raw_body)

        # HTTP API può passare il body già come dict oppure come JSON string
        if isinstance(raw_body, str):
            body = json.loads(raw_body)
        elif isinstance(raw_body, dict):
            body = raw_body
        else:
            body = {}

        user_message = body.get("text", "").strip()
        if not user_message:
            raise ValueError("Missing 'text' in request body.")

        headers = {
            "Authorization": f"Bearer {API_KEY}",
            "Content-Type": "application/json"
        }

        data = {
            "model": "gpt-3.5-turbo",
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT.strip()},
                {"role": "user", "content": user_message}
            ]
        }

        logger.info("Request to OpenAI: %s", json.dumps(data))

        res = requests.post(API_URL, headers=headers, json=data)
        res.raise_for_status()

        reply = res.json()["choices"][0]["message"]["content"]
        logger.info("OpenAI reply: %s", reply)

        return _response(200, {"reply": reply})

    except Exception as e:
        logger.exception("Exception caught")
        return _response(400, {"error": str(e)})

def _response(status, body):
    return {
        "statusCode": status,
        "body": json.dumps(body),
        "headers": {
            "Access-Control-Allow-Origin": "*"
        }
    }
