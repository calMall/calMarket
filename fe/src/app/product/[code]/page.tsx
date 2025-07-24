import { getProductDetail } from "@/api/Product";
import CustomLayout from "@/components/common/CustomLayout";
import ErrorComponent from "@/components/common/ErrorComponent";
import ProductDetailImage from "@/components/product/ProductDetailImage";
import ProductDetailOrReview from "@/components/product/ProductDetailOrReview";
import ProductDetailTitle from "@/components/product/ProductDetailTitle";

export default async function animeDetail({
  params,
}: {
  params: Promise<{ code: string }>;
}) {
  try {
    const { code } = await params;

    const data = await getProductDetail(code);

    return (
      <CustomLayout>
        <div className="product-detail-top-contain wf mt-2">
          <ProductDetailImage
            itemName={data.product.itemName}
            images={data.product.imageUrls}
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
          itemCode={data.product.itemCode}
          text={data.product.itemCaption}
        />
      </CustomLayout>
    );
  } catch (e) {
    console.log(e);
    return <ErrorComponent />;
  }
}
