import type { Metadata } from "next";
import "@/styles/style.scss";
import Header from "@/components/common/Header";
import { Suspense } from "react";
import { Footer } from "@/components/common/Footer";
import GoTop from "@/components/common/Gotop";

export const metadata: Metadata = {
  title: "キャルマ",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="jp">
      <body>
        <Suspense fallback={<div></div>}>
          <Header />
        </Suspense>
        {children}
        <Suspense fallback={<div></div>}>
          <GoTop />
          <Footer />
        </Suspense>
      </body>
    </html>
  );
}
