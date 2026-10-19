import { ScrollViewStyleReset } from 'expo-router/html';
import { type PropsWithChildren } from 'react';

/**
 * Root HTML layout for Expo Router Web.
 * 
 * Configures:
 * 1. Google Web Fonts: Plus Jakarta Sans (400, 500, 600, 700) & JetBrains Mono (400, 500, 600)
 * 2. CSS root variables: --font-sans and --font-mono
 * 3. Universal typography application and antialiasing across all elements
 */
export default function Root({ children }: PropsWithChildren) {
  return (
    <html lang="en">
      <head>
        <meta charSet="utf-8" />
        <meta httpEquiv="X-UA-Compatible" content="IE=edge" />
        <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />
        <title>KernelCraft • From Silicon to Syscall</title>

        {/* Global Import: Plus Jakarta Sans & JetBrains Mono */}
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        <link
          href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap"
          rel="stylesheet"
        />

        {/* Global Design Tokens & Universal Font-Family Resets */}
        <style
          dangerouslySetInnerHTML={{
            __html: `
              :root {
                --font-sans: 'Plus Jakarta Sans', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                --font-mono: 'JetBrains Mono', monospace;
              }

              *,
              html,
              body,
              button,
              input,
              textarea,
              select,
              [dir="auto"] {
                font-family: var(--font-sans);
                -webkit-font-smoothing: antialiased;
                -moz-osx-font-smoothing: grayscale;
              }

              code,
              pre,
              kbd,
              samp,
              .font-mono {
                font-family: var(--font-mono);
              }

              /* Hide native browser scrollbars globally while preserving scrollability */
              ::-webkit-scrollbar {
                display: none !important;
                width: 0px !important;
                height: 0px !important;
              }

              * {
                scrollbar-width: none !important;
                -ms-overflow-style: none !important;
              }
            `,
          }}
        />
        <ScrollViewStyleReset />
      </head>
      <body>{children}</body>
    </html>
  );
}
