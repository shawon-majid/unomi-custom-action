import indexHtml from "./index.html" with { type: "text" };

const PORT = Number(process.env.PORT ?? 3000);

Bun.serve({
  port: PORT,
  fetch(req) {
    const url = new URL(req.url);
    if (url.pathname === "/" || url.pathname === "/index.html") {
      return new Response(indexHtml, {
        headers: { "Content-Type": "text/html; charset=utf-8" },
      });
    }
    return new Response("not found", { status: 404 });
  },
});

console.log(`web → http://localhost:${PORT}`);
