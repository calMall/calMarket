"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import CustomLayout from "./CustomLayout";

export default function NoProduct() {
  const router = useRouter();

  useEffect(() => {
    alert("楽天市場にこの商品の詳細情報が存在しません");
    router.back();
  }, []);

  return (
    <CustomLayout>
      <></>
    </CustomLayout>
  );
}
