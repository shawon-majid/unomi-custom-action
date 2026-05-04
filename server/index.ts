const PORT = Number(process.env.PORT ?? 3001);

Bun.serve({
  port: PORT,
  async fetch(req) {
    const url = new URL(req.url);

    if (req.method === "GET" && url.pathname === "/") {
      return new Response("ok", { status: 200 });
    }

    if (req.method === "POST" && url.pathname === "/unomi-hook") {
      let body: unknown;
      try {
        body = await req.json();
      } catch {
        return new Response("bad json", { status: 400 });
      }

      const { count, profileId, timestamp } = body as {
        count?: number | string;
        profileId?: string;
        timestamp?: string;
      };

      const ts = timestamp ?? new Date().toISOString();
      console.log(
        `[unomi-hook] profile=${profileId ?? "?"} count=${count ?? "?"} at=${ts}`
      );

      return new Response("ok", { status: 200 });
    }

    return new Response("not found", { status: 404 });
  },
});

console.log(`server → http://localhost:${PORT}  (POST /unomi-hook)`);
