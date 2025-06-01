# voice-tutor-demo

**Minimal open-source demo** of the voice tutor module from the app  
[Curiosity Let the Cat Roam](https://www.curiositycat.xyz) – also available  
on [Google Play](https://play.google.com/store/apps/details?id=com.riccardo.curiosityletthecatroam).

This project shows how to build a **voice-based language tutor** using  
Android, AWS Lambda, and OpenAI.

## Contents

Included in this demo:

- `VoiceTutorActivity.java`: simple UI for chatting with the tutor  
- `VoiceTutorManager.java`: handles TTS, API calls, and response parsing  
- `VoiceTutorAdapter.java`: displays messages and plays them with TTS  
- `lambda_handler.py`: AWS Lambda function that sends input to OpenAI  
  and returns the response  
- **Basic facial animation** synchronized with speech output

## Requirements

To run this demo, you need:

- A [OpenAI account](https://platform.openai.com/)
- A valid API key (`OPENAI_API_KEY`)
- An **HTTP API Gateway endpoint** connected to the Lambda  
  (**not** a REST API)

> ⚠️ If you want to use a **REST API Gateway**, you'll need to adapt  
> the `lambda_handler.py` to match REST event structure.

## Setup

1. Clone this repository  
2. Edit `VoiceTutorManager.java` and set your API endpoint  
3. Deploy `lambda_handler.py` to AWS Lambda  
4. Set the `OPENAI_API_KEY` as a Lambda environment variable

## Notes

- This version has **no authentication** or access control  
- It's meant for educational/demo purposes only  
- Includes a **simple talking head animation**, synced with TTS

## License

Licensed under the **GNU GPL v3**.  
If you share a modified version, you must also share its source code.

## Full version

👉 Try the full app at  
[play.google.com/store/apps/details?id=com.riccardo.curiosityletthecatroam](https://play.google.com/store/apps/details?id=com.riccardo.curiosityletthecatroam)

👉 See all my apps at  
[play.google.com/store/apps/dev?id=5844635661190767943](https://play.google.com/store/apps/dev?id=5844635661190767943)

👉 Learn more at  
[curiositycat.xyz](https://www.curiositycat.xyz)
