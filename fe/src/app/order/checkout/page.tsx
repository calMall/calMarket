import CustomLayout from "@/components/common/CustomLayout";
import { Suspense } from "react";

export default function CheckoutOrder() {
  return (
    <CustomLayout>
      <Suspense>
        <CheckoutOrder />
      </Suspense>
    </CustomLayout>
  );
}
