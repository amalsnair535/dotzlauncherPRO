export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    // 1. Handle CORS Preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, User-Agent, Authorization",
        },
      });
    }

    // 2. Pure API Logic (Accepts root domain or /api)
    if (request.method === "POST") {
      try {
        let prompt = "";
        try {
          const body = await request.json();
          prompt = body.prompt || "";
        } catch (e) {
          return new Response(JSON.stringify({ text: "Error: Invalid JSON payload." }), {
            status: 400,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        if (!prompt) {
          return new Response(JSON.stringify({ text: "Please provide a prompt." }), {
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        // --- GROQ API INTEGRATION ---
        const apiKey = env.GROQ_API_KEY;
        if (!apiKey) {
          return new Response(JSON.stringify({ text: "AI Error: GROQ_API_KEY is not configured in worker secrets." }), {
            status: 500,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        const groqUrl = "https://api.groq.com/openai/v1/chat/completions";

        const response = await fetch(groqUrl, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${apiKey}`
          },
          body: JSON.stringify({
            model: "llama3-8b-8192",
            messages: [
              { role: "system", content: "You are Dotz AI, a minimalist assistant." },
              { role: "user", content: prompt }
            ],
            temperature: 0.7,
            max_tokens: 1024,
          })
        });

        const data = await response.json();

        if (data.error) {
          return new Response(JSON.stringify({ text: `Groq API Error: ${data.error.message}` }), {
            status: 500,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        const aiText = data.choices?.[0]?.message?.content || "No response generated.";

        return new Response(JSON.stringify({ text: aiText }), {
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*"
          },
        });

      } catch (e) {
        return new Response(JSON.stringify({ text: `Worker Exception: ${e.message}` }), {
          status: 500,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
        });
      }
    }

    // Default response for GET/Browser visits
    return new Response(JSON.stringify({ status: "Dotz AI API is running.", version: "1.0.0" }), {
      headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
    });
  },
};
