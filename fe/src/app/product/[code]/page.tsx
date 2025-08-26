import { getProductDetail } from "@/api/Product";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import NoProduct from "@/components/common/NoProduct";
import ProductDetailImage from "@/components/product/ProductDetailImage";
import ProductDetailOrReview from "@/components/product/ProductDetailOrReview";
import ProductDetailTitle from "@/components/product/ProductDetailTitle";

export default async function ProductDetail({
  params,
}: {
  params: Promise<{ code: string }>;
}) {
  try {
    const { code } = await params;

    const data = await getProductDetail(decodeURIComponent(code));

    return (
      <CustomLayout>
        <div className="product-detail-top-contain wf mt-2">
          <ProductDetailImage
            itemName={data.product.itemName}
            images={
              data.product.imageUrls && data.product.imageUrls.length > 0
                ? data.product.imageUrls
                : ["/No_Image.jpg"]
            }
          />
          <ProductDetailTitle
            code={data.product.itemCode}
            reviewCnt={data.product.reviewCount}
            itemName={data.product.itemName}
            price={data.product.price}
            explanation={data.product.catchcopy}
            rating={data.product.score}
          />
        </div>
        <ProductDetailOrReview
          rating={data.product.score}
          itemCode={data.product.itemCode}
          text={data.product.itemCaption}
        />
      </CustomLayout>
    );
  } catch (e) {
    return <NoProduct />;
  }
}
