(function() {
	return {
		onReady : function(data) {

			$select = $('#input-tags').selectize(
					{
						delimiter : ',',
						persist : false,
						create : function(input) {
							return {
								value : input,
								text : input
							}
						},
						create: true,
						valueField : 'email',
						labelField : 'name',
						options : [ {
							email : 'brian@thirdroute.com',
							name : 'Brian Reavis'
						}, {
							email : 'nikola@tesla.com',
							name : 'Nikola Tesla'
						}, {
							email : 'someone@gmail.com'
						} ],
						searchField: ['name', 'email'],
						/*
						render : {
							item : function(item, escape) {
								return '<div>'
										+ (item.name ? '<span class="name">'
												+ escape(item.name) + '</span>'
												: '')
										+ (item.email ? '<span class="email">'
												+ escape(item.email)
												+ '</span>' : '') + '</div>';
							},
							option : function(item, escape) {
								var label = item.name || item.email;
								var caption = item.name ? item.email : null;
								return '<div>'
										+ '<span class="label">'
										+ escape(label)
										+ '</span>'
										+ (caption ? '<span class="caption">'
												+ escape(caption) + '</span>'
												: '') + '</div>';
							}
						}
						*/
					});

			var selectize = $select[0].selectize;

			selectize.on('item_add', function(data) {
				console.log("You have just typed '" + data + "'");
			});
			
			selectize.on('type', function(data) {
				console.log("You are typing '" + data + "'");
			});

		},
	};
}());