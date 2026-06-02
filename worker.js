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

    // 2. Handle AI requests (POST)
    if (request.method === "POST") {
      try {
        let prompt = "";
        try {
          const body = await request.json();
          prompt = body.prompt || "";
        } catch (e) {
          return new Response(JSON.stringify({ text: "Error: Invalid JSON payload in request." }), {
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
          return new Response(JSON.stringify({ text: "AI Error: GROQ_API_KEY is not configured in the worker secrets. Please run 'npx wrangler secret put GROQ_API_KEY'." }), {
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
              { role: "system", content: "You are Dotz AI, a minimalist and helpful assistant for Dotz Launcher PRO." },
              { role: "user", content: prompt }
            ],
            temperature: 0.7,
            max_tokens: 1024,
          })
        });

        const data = await response.json();

        if (data.error) {
          return new Response(JSON.stringify({ text: `Groq API Error: ${data.error.message || "Unknown error"}` }), {
            status: 500,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
          });
        }

        const aiText = data.choices?.[0]?.message?.content || "I'm sorry, I couldn't generate a response.";

        return new Response(JSON.stringify({ text: aiText }), {
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*"
          },
        });

      } catch (e) {
        return new Response(JSON.stringify({ text: `AI Worker Exception: ${e.message}` }), {
          status: 500,
          headers: { "Access-Control-Allow-Origin": "*" }
        });
      }
    }

    // 3. Fallback to static assets
    return env.ASSETS.fetch(request);
  },
};
