const PORT = Number(process.env.PORT ?? 3000);
const indexFile = Bun.file(new URL("./index.html", import.meta.url));

Bun.serve({
  port: PORT,
  async fetch(req) {
    const url = new URL(req.url);
    if (url.pathname === "/" || url.pathname === "/index.html") {
      return new Response(await indexFile.text(), {
        headers: {
          "Content-Type": "text/html; charset=utf-8",
          "Cache-Control": "no-store",
        },
      });
    }
    return new Response("not found", { status: 404 });
  },
});

console.log(`web → http://localhost:${PORT}`);
